package xyz.ludothegreat.audiobooktv.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsAudioTrack
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import xyz.ludothegreat.audiobooktv.data.log.DiagnosticLog
import xyz.ludothegreat.audiobooktv.data.settings.AppSettings
import xyz.ludothegreat.audiobooktv.data.settings.PositionCacheStore
import xyz.ludothegreat.audiobooktv.data.settings.SpeedStore
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import xyz.ludothegreat.audiobooktv.playback.BookmarkList
import xyz.ludothegreat.audiobooktv.playback.BookmarksRepository
import xyz.ludothegreat.audiobooktv.playback.ChapterMath
import xyz.ludothegreat.audiobooktv.playback.PauseFlushGate
import xyz.ludothegreat.audiobooktv.playback.PlaybackPrep
import xyz.ludothegreat.audiobooktv.playback.PlaybackRepository
import xyz.ludothegreat.audiobooktv.playback.PlayerService
import xyz.ludothegreat.audiobooktv.playback.PositionMath
import xyz.ludothegreat.audiobooktv.playback.PositionReconciler
import xyz.ludothegreat.audiobooktv.playback.ResumeDecision
import xyz.ludothegreat.audiobooktv.playback.RetryPolicy
import xyz.ludothegreat.audiobooktv.playback.ScrubTargets
import xyz.ludothegreat.audiobooktv.playback.SeekCause
import xyz.ludothegreat.audiobooktv.playback.SeekHistory
import xyz.ludothegreat.audiobooktv.playback.SeekTargets
import xyz.ludothegreat.audiobooktv.playback.SleepCountdown
import xyz.ludothegreat.audiobooktv.playback.SleepEocGate
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms
import javax.inject.Inject

data class PlayerUiState(
    val loading: Boolean = false,
    val itemId: String? = null,
    val title: String = "",
    val author: String = "",
    val chapterTitle: String = "",
    val coverUrl: String? = null,
    val positionSec: Long = 0,
    val durationSec: Long = 0,
    val isPlaying: Boolean = false,
    val speed: Float = 1.0f,
    val speedPanelVisible: Boolean = false,
    val bookmarkPanelVisible: Boolean = false,
    /**
     * Chapter records from the play-session payload, empty when the book has
     * none. The surfaces derive the current chapter, its bar fill, and the
     * negative remaining label from this plus positionSec via ChapterMath, so
     * there is no per-tick chapter state to keep in sync here.
     */
    val chapters: List<AbsChapter> = emptyList(),
    val chapterPanelVisible: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val bookmarksLoading: Boolean = false,
    val isReconnecting: Boolean = false,
    val error: String? = null,
    /** User's selected sleep-timer preset, in whole minutes. 0 = Off. */
    val sleepTimerMinutes: Int = 0,
    /** Live countdown remaining in seconds, or null when no timer is ticking. */
    val sleepTimerRemainingSec: Long? = null,
    /**
     * Sleep-timer end-of-chapter SETTING, straight from DataStore. The
     * surfaces derive the effective mode per book (`&& chapters exist`)
     * because a chapter stop can never fire on a chapterless book and the
     * label must not promise one.
     */
    val sleepEndOfChapter: Boolean = false,
    /** True while an armed chapter stop is running out the current chapter. */
    val sleepEocWaiting: Boolean = false,
    val sleepTimerPanelVisible: Boolean = false,
    /** Where undoSeek() would take the player, or null when there is nothing to undo. */
    val undoSeekTargetSec: Long? = null,
    /** Transient per-seek notice for the touch snackbar; TV shows the undo row instead. */
    val seekNotice: SeekNotice? = null,
    /** Transient outcome of the last bookmark rename/delete for snackbar/panel display. */
    val bookmarkNotice: BookmarkNotice? = null,
)

/**
 * seq makes each notice distinct so a LaunchedEffect keyed on it re-fires for
 * every seek; atMs lets the UI drop a notice that is stale by the time the
 * screen re-enters composition (tab switches replay the latest state).
 */
data class SeekNotice(
    val seq: Long,
    val toSec: Long,
    val atMs: Long,
)

data class BookmarkNotice(
    val seq: Long,
    val text: String,
    val isError: Boolean,
    val atMs: Long,
)

val SPEED_PRESETS: List<Float> = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

/**
 * Sleep-timer presets exposed in the panel. 0 represents "Off". Pulled
 * out as a top-level constant so the unit tests and the panel UI use
 * the same list.
 */
val SLEEP_TIMER_PRESETS_MINUTES: List<Int> = listOf(0, 5, 10, 15, 30, 45, 60)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val speedStore: SpeedStore,
    private val appSettings: AppSettings,
    private val positionCacheStore: PositionCacheStore,
    private val diagnosticLog: DiagnosticLog,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val controllerReady = MutableStateFlow(false)
    private var chapters: List<AbsChapter> = emptyList()
    private var tracks: List<AbsAudioTrack> = emptyList()
    private var sessionId: String? = null
    private var pendingLoad: Pair<String, String?>? = null
    private var syncJob: Job? = null
    private var pausedPollJob: Job? = null
    private var retryJob: Job? = null
    private var firstErrorWallClockMs: Long = 0
    private var lastSyncWallClockMs: Long = 0
    private val seekHistory = SeekHistory()
    private val sleepEocGate = SleepEocGate()
    private val pauseFlushGate = PauseFlushGate()
    private var noticeSeq: Long = 0

    private val sleepCountdown = SleepCountdown(
        scope = viewModelScope,
        onFire = { onSleepCountdownFired() },
        onRemainingChanged = { remainingMs ->
            _state.update { it.copy(sleepTimerRemainingSec = remainingMs?.div(1000)) }
        },
    )

    private val foregroundObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // App returned to foreground. If another client (ABS web, phone)
            // has advanced the position while we were away, pull it in.
            val id = _state.value.itemId ?: return
            refreshFromServer(id)
        }
    }

    init {
        bindController()
        startTicker()
        observeSleepTimerPreset()
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundObserver)
    }

    private fun observeSleepTimerPreset() {
        viewModelScope.launch {
            appSettings.sleepTimerMinutes.collect { minutes ->
                val prev = _state.value.sleepTimerMinutes
                _state.update { it.copy(sleepTimerMinutes = minutes) }
                // Reload the countdown's duration when the user changes the
                // preset OR when the saved preset is hydrated on cold start.
                if (minutes != prev) {
                    sleepCountdown.setDuration(minutes)
                    if (controller?.isPlaying == true) sleepCountdown.resume()
                    // A positive preset restarts the sequence from the
                    // countdown, so any already-armed chapter stop (the
                    // combined mode's second phase) is superseded.
                    if (minutes > 0) {
                        sleepEocGate.disarm()
                        _state.update { it.copy(sleepEocWaiting = false) }
                    }
                }
            }
        }
        viewModelScope.launch {
            appSettings.sleepEndOfChapter.collect { eoc ->
                val prev = _state.value.sleepEndOfChapter
                _state.update { it.copy(sleepEndOfChapter = eoc) }
                if (eoc == prev) return@collect
                if (!eoc) {
                    // Off switch for the chapter stop: a stop that is already
                    // running out its chapter is cancelled too, not just the
                    // next arming.
                    sleepEocGate.disarm()
                    _state.update { it.copy(sleepEocWaiting = false) }
                } else if (
                    controller?.isPlaying == true &&
                    _state.value.sleepTimerMinutes == 0 &&
                    chapters.isNotEmpty()
                ) {
                    // End-of-chapter-only picked mid-play: arm immediately.
                    // With minutes > 0 the running countdown arms the gate
                    // itself when it fires.
                    sleepEocGate.arm(currentChapterIndex())
                    _state.update { it.copy(sleepEocWaiting = true) }
                }
            }
        }
    }

    /**
     * The countdown ran out. Plain mode pauses on the spot. Combined mode
     * hands over to the chapter gate so playback runs out the chapter the
     * user is hearing -- unless the book has no chapters, in which case
     * arming a gate that can never fire would turn the sleep timer into a
     * silent no-op, so it degrades to the plain pause.
     */
    private fun onSleepCountdownFired() {
        if (_state.value.sleepEndOfChapter && chapters.isNotEmpty()) {
            sleepEocGate.arm(currentChapterIndex())
            _state.update { it.copy(sleepEocWaiting = true) }
        } else {
            controller?.pause()
        }
    }

    private fun currentChapterIndex(): Int? {
        val ctl = controller ?: return null
        return ChapterMath.indexAt(absolutePositionSec(ctl).toDouble(), chapters)
    }

    private fun bindController() {
        val token = SessionToken(context, ComponentName(context, PlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { ctl ->
                if (!ctl.isPlaying) startPausedPoll()
                ctl.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            stopPausedPoll()
                            lastSyncWallClockMs = System.currentTimeMillis()
                            startSyncTimer()
                            sleepCountdown.resume()
                            val st = _state.value
                            val eocOnly = st.sleepEndOfChapter &&
                                st.sleepTimerMinutes == 0 &&
                                chapters.isNotEmpty()
                            if (eocOnly && !sleepEocGate.armed) {
                                // End-of-chapter-only mode arms on every play,
                                // mirroring how the countdown presets re-arm.
                                sleepEocGate.arm(currentChapterIndex())
                                _state.update { it.copy(sleepEocWaiting = true) }
                            } else {
                                // An armed stop survives a pause; re-target it
                                // in case a paused-poll sync moved the head.
                                sleepEocGate.reseed(currentChapterIndex())
                            }
                        } else {
                            stopSyncTimer()
                            // Flush one last sync on pause so server sees the
                            // current position even if the user stops in
                            // mid-interval. The gate suppresses exactly the
                            // stop that a book switch produces: that callback
                            // arrives after load() has installed the NEW
                            // session and track table, so flushing here would
                            // write the old book's playhead into the new
                            // book's progress (cross-book bleed, case C1).
                            // load() flushes the old session itself first.
                            if (pauseFlushGate.shouldFlushOnPause()) syncOnce()
                            // Start polling so another client's progress
                            // shows up here without needing the user to
                            // background the app.
                            startPausedPoll()
                            // Pause the countdown but keep the remaining time
                            // so a subsequent play resumes from where we left.
                            sleepCountdown.pause()
                        }
                    }
                    override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                        _state.update { it.copy(speed = params.speed) }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        diagnosticLog.w("Player", "ExoPlayer error: ${error.errorCodeName} ${error.message}", error)
                        handlePlayerError()
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            // Successful playback after error means we're back.
                            firstErrorWallClockMs = 0
                            retryJob?.cancel()
                            retryJob = null
                            if (_state.value.isReconnecting) {
                                _state.update { it.copy(isReconnecting = false) }
                            }
                        }
                    }
                })
                _state.update {
                    it.copy(
                        isPlaying = ctl.isPlaying,
                        speed = ctl.playbackParameters.speed,
                    )
                }
            }
            controllerReady.value = true
            pendingLoad?.let { (id, cover) ->
                pendingLoad = null
                load(id, cover)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(500)
                val ctl = controller ?: continue
                if (ctl.duration <= 0) continue
                val absSec = absolutePositionSec(ctl)
                if (sleepEocGate.armed &&
                    ctl.isPlaying &&
                    sleepEocGate.onChapterTick(ChapterMath.indexAt(absSec.toDouble(), chapters))
                ) {
                    // The chapter the sleep gate was tracking just ended.
                    ctl.pause()
                    _state.update { it.copy(sleepEocWaiting = false) }
                }
                val current = _state.value
                val newChapter = currentChapterTitle(absSec.toDouble())
                // Only emit a state change when something the user can see has
                // actually changed. Whole-second positionSec means 3 of every 4
                // ticks at the old 250ms interval were pure no-op recompositions.
                if (absSec != current.positionSec || newChapter != current.chapterTitle) {
                    _state.update { it.copy(positionSec = absSec, chapterTitle = newChapter) }
                }
            }
        }
    }

    fun load(itemId: String, coverUrl: String?) {
        if (_state.value.itemId == itemId && (controller?.mediaItemCount ?: 0) > 0) {
            // Same book already loaded - don't rebuild the playlist, but do
            // pull the latest position from the server in case another client
            // advanced it while we were elsewhere.
            refreshFromServer(itemId)
            return
        }
        // Loading a different book invalidates the seek history: a fromSec
        // from another book is a plausible-looking position here, and undoing
        // to it would push a bogus seek to the server.
        seekHistory.clear()
        _state.update { it.copy(undoSeekTargetSec = null) }
        if (!controllerReady.value) {
            pendingLoad = itemId to coverUrl
            sleepEocGate.disarm()
            _state.update {
                it.copy(
                    loading = true,
                    itemId = itemId,
                    coverUrl = coverUrl,
                    positionSec = 0,
                    durationSec = 0,
                    chapters = emptyList(),
                    sleepEocWaiting = false,
                    error = null,
                )
            }
            return
        }
        flushAndClosePreviousSession()
        // New playback resets the sleep timer to its selected preset value.
        // If the timer was at 5 min remaining of a 30-min preset and the
        // user opens a different book, the next playback gets a fresh 30 min.
        // Same for the chapter gate: an armed stop is stale against the new
        // book's chapter list, so it disarms and re-arms per the setting
        // when playback starts.
        sleepCountdown.setDuration(_state.value.sleepTimerMinutes)
        sleepEocGate.disarm()
        // Clear the previous book's chapters AND position envelope for the
        // whole load window. Stale chapter data against the new book's
        // positions would light up a wrong (or ghost) chapter bar, and a
        // stale durationSec is worse: a skip pressed mid-load would clamp
        // against the previous book's duration and push a bogus position
        // into the NEW session. Zeroing durationSec makes ScrubTargets.clamp
        // null-drop every user seek until openPlayback delivers the truth.
        _state.update {
            it.copy(
                loading = true,
                itemId = itemId,
                coverUrl = coverUrl,
                positionSec = 0,
                durationSec = 0,
                chapters = emptyList(),
                sleepEocWaiting = false,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { playbackRepository.openPlayback(itemId) }
                .onSuccess { prep -> onPlaybackOpened(itemId, prep) }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, error = t.message ?: "Failed to open playback.") }
                }
        }
    }

    /**
     * Close any previous server session before opening a new one for a
     * different book so ABS doesn't hold a dangling session per device.
     * If the old book is still PLAYING, flush its position into the OLD
     * session here, with the OLD track table, and arm the gate: the
     * isPlaying=false that setMediaItems will trigger arrives only after
     * the new session is installed, and a pause flush at that moment
     * attributes the old playhead to the new book (case C1, observed
     * live: a fresh book jumped to the previous book's 12435s).
     */
    private fun flushAndClosePreviousSession() {
        val previous = sessionId ?: return
        val ctlNow = controller
        val previousItemId = _state.value.itemId
        val previousDuration = _state.value.durationSec.toDouble()
        val flushSec = if (ctlNow?.isPlaying == true) absolutePositionSec(ctlNow).toDouble() else null
        if (flushSec != null) pauseFlushGate.armForTransition()
        sessionId = null
        stopSyncTimer()
        val now = System.currentTimeMillis()
        val deltaSec = if (lastSyncWallClockMs > 0) ((now - lastSyncWallClockMs) / 1000.0) else 0.0
        lastSyncWallClockMs = now
        viewModelScope.launch {
            if (flushSec != null && previousItemId != null) {
                syncTracked(
                    sessionId = previous,
                    itemId = previousItemId,
                    positionSec = flushSec,
                    listenedSec = deltaSec.coerceAtLeast(0.0),
                    durationSec = previousDuration,
                )
            }
            playbackRepository.closeSession(previous)
        }
    }

    private suspend fun onPlaybackOpened(itemId: String, prep: PlaybackPrep) {
        sessionId = prep.session.id
        chapters = prep.session.chapters
        tracks = prep.session.audioTracks
        val ctl = controller
        if (ctl == null) {
            _state.update { it.copy(loading = false, error = "Player not ready.") }
            return
        }
        // Resume position: server-truth, except when a DIRTY local record
        // outran the server while syncs were failing (crash during a network
        // drop, case E). Then the local value is adopted AND pushed so the
        // server converges forward instead of this device seeking back over
        // real progress.
        val serverResumeSec = prep.resumePositionMs / 1000.0
        val bookDurationSec = prep.session.duration
        val decision = PositionReconciler.reconcile(
            itemId = itemId,
            serverSec = serverResumeSec,
            durationSec = bookDurationSec,
            local = positionCacheStore.read(),
        )
        val resumeSec = decision.positionSec
        ctl.setMediaItems(prep.mediaItems, false)
        ctl.prepare()
        seekToAbsoluteMs((resumeSec * 1000).toLong())
        val savedSpeed = speedStore.get(itemId) ?: 1.0f
        ctl.setPlaybackSpeed(savedSpeed)
        _state.update {
            it.copy(
                loading = false,
                title = prep.session.displayTitle ?: "",
                author = prep.session.displayAuthor ?: "",
                durationSec = bookDurationSec.toLong(),
                positionSec = resumeSec.toLong(),
                chapters = prep.session.chapters,
                chapterTitle = currentChapterTitle(resumeSec),
                isPlaying = ctl.isPlaying,
                speed = savedSpeed,
            )
        }
        if (decision is ResumeDecision.UseLocalAndPush) {
            diagnosticLog.i(
                "Player",
                "Recovered unsynced local position ${resumeSec}s over server ${serverResumeSec}s for $itemId",
            )
            pushPositionToServer(resumeSec, bookDurationSec)
        }
        // Land paused -> start polling so external client changes
        // converge without requiring the user to press Play.
        if (!ctl.isPlaying) startPausedPoll()
    }

    fun togglePlayPause() {
        val ctl = controller ?: return
        if (ctl.isPlaying) {
            ctl.pause()
            return
        }
        val id = _state.value.itemId
        if (id == null) {
            ctl.play()
            return
        }
        // Pre-play check: pull the latest server position. If another client
        // (ABS web on a phone, another TV) advanced past our local position,
        // start from there instead of replaying. Reconciled first: a dirty
        // local record ahead of the server means the server value is stale,
        // not authoritative, and adopting it would replay listened audio.
        viewModelScope.launch {
            val serverSec = playbackRepository.fetchSavedPositionSec(id)
            val ctlNow = controller ?: return@launch
            if (serverSec != null) {
                val adoptSec = adoptServerPosition(id, serverSec)
                val localSec = absolutePositionSec(ctlNow).toDouble()
                if (kotlin.math.abs(adoptSec - localSec) > POSITION_DRIFT_TOLERANCE_SEC) {
                    seekToAbsoluteMs((adoptSec * 1000).toLong())
                    _state.update {
                        it.copy(
                            positionSec = adoptSec.toLong(),
                            chapterTitle = currentChapterTitle(adoptSec),
                        )
                    }
                }
            }
            controller?.play()
        }
    }

    // Skip handlers ride userSeekTo, the single clamp-seek-push-record path.
    // They used to seek and push directly, which left a hole the v1.2.1
    // progress-wipe fix closed for scrubs but not for skips: with duration
    // still unknown, SeekTargets.skipForward coerces to 0 and the push wipes
    // the listener's server position. ScrubTargets.clamp inside userSeekTo
    // null-drops any skip until durationSec > 0, and the push it does keep
    // stops the pre-play refresh in togglePlayPause from snapping back.
    fun skipBack30() {
        val ctl = controller ?: return
        userSeekTo(SeekTargets.skipBack(absolutePositionSec(ctl)), SeekCause.Skip)
    }

    fun skipForward30() {
        val ctl = controller ?: return
        userSeekTo(SeekTargets.skipForward(absolutePositionSec(ctl), _state.value.durationSec), SeekCause.Skip)
    }

    fun skipBackLong() {
        val ctl = controller ?: return
        userSeekTo(SeekTargets.longSkipBack(absolutePositionSec(ctl)), SeekCause.LongSkip)
    }

    fun skipForwardLong() {
        val ctl = controller ?: return
        userSeekTo(SeekTargets.longSkipForward(absolutePositionSec(ctl), _state.value.durationSec), SeekCause.LongSkip)
    }

    /**
     * Touch-scrubber commit point. Thin wrapper over [userSeekTo] so the
     * scrub rides the same clamp-seek-push-record path as every other
     * user seek (skip-30, bookmark jump, chapter jump, undo); only the
     * recorded cause differs.
     */
    fun seekToAbsoluteSec(seconds: Long) {
        userSeekTo(seconds, SeekCause.Scrub)
    }

    /**
     * Undo the last user seek by seeking back to where it came from. This is
     * itself a normal user seek through the clamp-seek-push path, recorded in
     * the history like any other, which is exactly what makes a second press
     * redo (see SeekHistory).
     */
    fun undoSeek() {
        val record = seekHistory.peekUndo() ?: return
        userSeekTo(record.fromSec, SeekCause.Undo)
    }

    private fun userSeekTo(seconds: Long, cause: SeekCause) {
        val ctl = controller ?: return
        // null = duration not known yet. Dropping the seek is the whole point:
        // falling through would push position 0 to the server.
        val target = ScrubTargets.clamp(targetSec = seconds, durationSec = _state.value.durationSec)
            ?: return
        val fromSec = absolutePositionSec(ctl)
        seekToAbsoluteMs(target * 1000)
        _state.update {
            it.copy(
                positionSec = target,
                chapterTitle = currentChapterTitle(target.toDouble()),
            )
        }
        pushPositionToServer(target.toDouble())
        recordUserSeek(fromSec = fromSec, toSec = target, cause = cause)
        // A deliberate jump is not a chapter ending: an armed sleep gate
        // re-targets to the chapter the user just landed in.
        sleepEocGate.reseed(ChapterMath.indexAt(target.toDouble(), chapters))
    }

    private fun recordUserSeek(fromSec: Long, toSec: Long, cause: SeekCause) {
        val now = System.currentTimeMillis()
        if (!seekHistory.record(fromSec = fromSec, toSec = toSec, cause = cause, atMs = now)) return
        _state.update {
            it.copy(
                undoSeekTargetSec = seekHistory.peekUndo()?.fromSec,
                seekNotice = SeekNotice(seq = ++noticeSeq, toSec = toSec, atMs = now),
            )
        }
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
        val id = _state.value.itemId ?: return
        viewModelScope.launch { speedStore.set(id, speed) }
    }

    fun openSpeedPanel() {
        _state.update { it.copy(speedPanelVisible = true) }
    }

    fun closeSpeedPanel() {
        _state.update { it.copy(speedPanelVisible = false) }
    }

    fun openSleepTimerPanel() {
        _state.update { it.copy(sleepTimerPanelVisible = true) }
    }

    fun closeSleepTimerPanel() {
        _state.update { it.copy(sleepTimerPanelVisible = false) }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        viewModelScope.launch {
            appSettings.setSleepTimerMinutes(minutes)
            // Off is the master off switch: it clears the end-of-chapter
            // flag too, so no automatic pause of any kind survives an
            // explicit Off. The eoc collector disarms the gate on the
            // emission.
            // Any explicit preset choice resets the mode to plain minutes.
            // Combining is then an opt-in on the subordinate toggle, so the
            // three modes (minutes / chapter end / minutes-then-chapter-end)
            // are never ambiguous in the picker.
            appSettings.setSleepEndOfChapter(false)
        }
        // observeSleepTimerPreset() will re-apply the duration on the
        // DataStore emission. Resume eagerly if currently playing so the
        // user sees the count start immediately rather than after a tick.
        if (controller?.isPlaying == true && minutes > 0) {
            sleepCountdown.setDuration(minutes)
            sleepCountdown.resume()
        }
    }

    fun setSleepEndOfChapter(enabled: Boolean) {
        // The DataStore emission drives arming/disarming in one place
        // (observeSleepTimerPreset's eoc collector).
        viewModelScope.launch { appSettings.setSleepEndOfChapter(enabled) }
    }

    /**
     * Pick end-of-chapter as its own exclusive mode: no minutes floor, stop
     * at the next boundary. Clearing the preset is what makes the picker
     * show exactly one selected row; leaving a stale preset selected next to
     * it is what made a working feature read as broken.
     */
    fun selectSleepEndOfChapterOnly() {
        viewModelScope.launch {
            appSettings.setSleepTimerMinutes(0)
            appSettings.setSleepEndOfChapter(true)
        }
    }

    fun openChapterPanel() {
        _state.update { it.copy(chapterPanelVisible = true) }
    }

    fun closeChapterPanel() {
        _state.update { it.copy(chapterPanelVisible = false) }
    }

    /**
     * Chapter jump is a user-initiated seek, so it must ride the existing
     * clamp-seek-push path -- userSeekTo drops the jump entirely while
     * duration is unknown (nullable-clamp contract), pushes the landed
     * position to the server, and records it in the seek history under its
     * own cause so undo can offer the pre-jump position. No parallel seek
     * path here.
     */
    fun jumpToChapter(chapter: AbsChapter) {
        userSeekTo(ChapterMath.jumpTargetSec(chapter), SeekCause.ChapterJump)
    }

    fun openBookmarkPanel() {
        val id = _state.value.itemId ?: return
        // A notice from the previous open would otherwise reappear as a
        // stale error line in the panel.
        _state.update { it.copy(bookmarkPanelVisible = true, bookmarksLoading = true, bookmarkNotice = null) }
        viewModelScope.launch {
            val items = BookmarkList.normalize(bookmarksRepository.fetchForItem(id))
            _state.update { it.copy(bookmarks = items, bookmarksLoading = false) }
        }
    }

    fun closeBookmarkPanel() {
        _state.update { it.copy(bookmarkPanelVisible = false) }
    }

    fun addBookmarkHere() {
        val id = _state.value.itemId ?: return
        val ctl = controller ?: return
        val timeSec = absolutePositionSec(ctl)
        val label = formatTimestampHms(timeSec)
        viewModelScope.launch {
            val created = bookmarksRepository.create(id, timeSec, label) ?: return@launch
            _state.update {
                it.copy(bookmarks = BookmarkList.normalize(it.bookmarks + created))
            }
        }
    }

    fun jumpToBookmark(bookmark: Bookmark) {
        // Also a user-initiated seek: userSeekTo supplies the controller
        // guard, the nullable clamp (dropping the jump while duration is
        // unknown instead of pushing a position the player never reached),
        // the server push, and the history record.
        userSeekTo(bookmark.timeSec, SeekCause.BookmarkJump)
    }

    /**
     * Server first, then local state through the normalizer. On failure the
     * list is left exactly as it was and the user is told loudly; quietly
     * showing a rename that the server never saw would be a lie that survives
     * until the next fetch.
     */
    fun renameBookmark(bookmark: Bookmark, newTitle: String) {
        val id = _state.value.itemId ?: return
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty() || trimmed == bookmark.title) return
        viewModelScope.launch {
            bookmarksRepository.rename(itemId = id, timeSec = bookmark.timeSec, title = trimmed)
                .onSuccess { renamed ->
                    _state.update {
                        it.copy(
                            bookmarks = BookmarkList.normalize(
                                it.bookmarks.map { b -> if (BookmarkList.sameBookmark(b, renamed)) renamed else b },
                            ),
                            bookmarkNotice = bookmarkNotice("Bookmark renamed", isError = false),
                        )
                    }
                }
                .onFailure { t ->
                    diagnosticLog.w("Player", "Bookmark rename failed: ${t.message}", t)
                    _state.update {
                        it.copy(bookmarkNotice = bookmarkNotice("Rename failed, bookmark unchanged", isError = true))
                    }
                }
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        val id = _state.value.itemId ?: return
        viewModelScope.launch {
            bookmarksRepository.delete(itemId = id, timeSec = bookmark.timeSec)
                .onSuccess {
                    _state.update {
                        it.copy(
                            bookmarks = BookmarkList.normalize(
                                it.bookmarks.filterNot { b -> BookmarkList.sameBookmark(b, bookmark) },
                            ),
                            bookmarkNotice = bookmarkNotice("Bookmark deleted", isError = false),
                        )
                    }
                }
                .onFailure { t ->
                    diagnosticLog.w("Player", "Bookmark delete failed: ${t.message}", t)
                    _state.update {
                        it.copy(bookmarkNotice = bookmarkNotice("Delete failed, bookmark kept", isError = true))
                    }
                }
        }
    }

    private fun bookmarkNotice(text: String, isError: Boolean) = BookmarkNotice(
        seq = ++noticeSeq,
        text = text,
        isError = isError,
        atMs = System.currentTimeMillis(),
    )

    /**
     * User-initiated seek: tell the server this is our new position so the
     * pre-play refresh in togglePlayPause doesn't snap us back to a stale
     * "where you last paused" value when the user hits Play next.
     *
     * durationSec is overridable for the one caller (load's reconcile push)
     * that runs before the state update lands the new book's duration.
     */
    private fun pushPositionToServer(timeSec: Double, durationSec: Double = _state.value.durationSec.toDouble()) {
        val sid = sessionId ?: return
        val id = _state.value.itemId ?: return
        viewModelScope.launch {
            syncTracked(
                sessionId = sid,
                itemId = id,
                positionSec = timeSec,
                listenedSec = 0.0,
                durationSec = durationSec,
            )
        }
    }

    /**
     * The single tracked write path to the server's progress record: dirty
     * local record BEFORE the network call, clean only after the server
     * confirmed. Every progress write (10s tick, pause flush, seek push,
     * book-switch flush, reconcile push) goes through here, so a crash or a
     * dead network at any instant leaves a dirty record for the reconciler
     * to replay. Losing the failure silently was case E: 27s of listening
     * gone to a force-stop during a network drop.
     */
    private suspend fun syncTracked(
        sessionId: String,
        itemId: String,
        positionSec: Double,
        listenedSec: Double,
        durationSec: Double,
    ): Boolean {
        positionCacheStore.writeDirty(itemId, positionSec)
        val confirmed = playbackRepository.syncProgress(
            sessionId = sessionId,
            currentTimeSec = positionSec,
            timeListenedSec = listenedSec,
            durationSec = durationSec,
        )
        if (confirmed) {
            positionCacheStore.markClean(itemId, positionSec)
        } else {
            diagnosticLog.w("Player", "Progress sync unconfirmed; dirty local record kept at ${positionSec}s for $itemId")
        }
        return confirmed
    }

    /**
     * Filter every externally fetched position through the reconciler before
     * adopting it. Returns the position to adopt; when a dirty local record
     * outran the server, that record wins and is pushed so the server
     * converges forward. Without this, the paused poll and the pre-play
     * refresh would seek BACKWARD onto a stale server value the moment the
     * network returns after a drop.
     */
    private suspend fun adoptServerPosition(itemId: String, serverSec: Double): Double {
        val decision = PositionReconciler.reconcile(
            itemId = itemId,
            serverSec = serverSec,
            durationSec = _state.value.durationSec.toDouble(),
            local = positionCacheStore.read(),
        )
        if (decision is ResumeDecision.UseLocalAndPush) {
            diagnosticLog.i(
                "Player",
                "Pushing unsynced local position ${decision.positionSec}s over server ${serverSec}s for $itemId",
            )
            pushPositionToServer(decision.positionSec)
        }
        return decision.positionSec
    }

    private fun absolutePositionSec(ctl: MediaController): Long = PositionMath.absolutePositionSec(
        currentPositionMs = ctl.currentPosition,
        currentMediaItemIndex = ctl.currentMediaItemIndex,
        tracks = tracks,
    )

    private fun seekToAbsoluteMs(absoluteMs: Long) {
        val ctl = controller ?: return
        val target = PositionMath.trackForAbsolute(absoluteMs, tracks)
        ctl.seekTo(target.mediaItemIndex, target.positionMs)
    }

    private fun currentChapterTitle(absSec: Double): String = PositionMath.currentChapterTitle(absSec, chapters)

    private fun refreshFromServer(itemId: String) {
        // Only pull a remote position if the local player isn't actively
        // playing - yanking the head backward mid-listen is worse than a
        // small drift, and any active play is already syncing to ABS itself.
        val ctl = controller ?: return
        if (ctl.isPlaying) return
        viewModelScope.launch {
            val serverSec = playbackRepository.fetchSavedPositionSec(itemId) ?: return@launch
            val adoptSec = adoptServerPosition(itemId, serverSec)
            val localSec = absolutePositionSec(ctl).toDouble()
            if (kotlin.math.abs(adoptSec - localSec) > POSITION_DRIFT_TOLERANCE_SEC) {
                seekToAbsoluteMs((adoptSec * 1000).toLong())
                _state.update {
                    it.copy(
                        positionSec = adoptSec.toLong(),
                        chapterTitle = currentChapterTitle(adoptSec),
                    )
                }
            }
        }
    }

    private fun handlePlayerError() {
        if (firstErrorWallClockMs == 0L) firstErrorWallClockMs = System.currentTimeMillis()
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            while (true) {
                delay(RetryPolicy.RETRY_INTERVAL_MS)
                val ctl = controller ?: continue
                // After RetryPolicy.RECONNECTING_BANNER_AFTER_MS of failed
                // retries, surface the small badge on the player so the user
                // knows we haven't forgotten -- but only then. Below that we
                // stay silent.
                val shouldShow = RetryPolicy.shouldShowReconnecting(
                    firstErrorWallClockMs = firstErrorWallClockMs,
                    nowMs = System.currentTimeMillis(),
                )
                if (shouldShow && !_state.value.isReconnecting) {
                    _state.update { it.copy(isReconnecting = true) }
                }
                // Kick the player. If still no network, onPlayerError fires
                // again and we loop. If it succeeds, onPlaybackStateChanged
                // (STATE_READY) clears the badge and cancels this job.
                ctl.prepare()
            }
        }
    }

    private fun startPausedPoll() {
        pausedPollJob?.cancel()
        pausedPollJob = viewModelScope.launch {
            while (true) {
                delay(PAUSED_POLL_INTERVAL_MS)
                val id = _state.value.itemId ?: continue
                refreshFromServer(id)
            }
        }
    }

    private fun stopPausedPoll() {
        pausedPollJob?.cancel()
        pausedPollJob = null
    }

    private fun startSyncTimer() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (true) {
                delay(SYNC_INTERVAL_MS)
                syncOnce()
            }
        }
    }

    private fun stopSyncTimer() {
        syncJob?.cancel()
        syncJob = null
    }

    private fun syncOnce() {
        val sid = sessionId ?: return
        val ctl = controller ?: return
        val id = _state.value.itemId ?: return
        val currentSec = absolutePositionSec(ctl).toDouble()
        val now = System.currentTimeMillis()
        val deltaSec = if (lastSyncWallClockMs > 0) ((now - lastSyncWallClockMs) / 1000.0) else 0.0
        lastSyncWallClockMs = now
        val duration = _state.value.durationSec.toDouble()
        viewModelScope.launch {
            syncTracked(
                sessionId = sid,
                itemId = id,
                positionSec = currentSec,
                listenedSec = deltaSec.coerceAtLeast(0.0),
                durationSec = duration,
            )
        }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(foregroundObserver)
        stopSyncTimer()
        stopPausedPoll()
        retryJob?.cancel()
        retryJob = null
        val sid = sessionId
        sessionId = null
        controller?.release()
        controller = null
        // Fire-and-forget close so ABS reflects the final position even when
        // the ViewModel is torn down. viewModelScope is already cancelled so
        // we spin up a short-lived scope just for this network call.
        if (sid != null) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                playbackRepository.closeSession(sid)
            }
        }
        super.onCleared()
    }

    companion object {
        private const val SYNC_INTERVAL_MS = 10_000L
        private const val PAUSED_POLL_INTERVAL_MS = 15_000L
        private const val POSITION_DRIFT_TOLERANCE_SEC = 3.0
    }
}
