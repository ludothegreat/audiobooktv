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
import xyz.ludothegreat.audiobooktv.data.settings.SpeedStore
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import xyz.ludothegreat.audiobooktv.playback.BookmarksRepository
import xyz.ludothegreat.audiobooktv.playback.PlaybackRepository
import xyz.ludothegreat.audiobooktv.playback.PlayerService
import xyz.ludothegreat.audiobooktv.playback.PositionMath
import xyz.ludothegreat.audiobooktv.playback.RetryPolicy
import xyz.ludothegreat.audiobooktv.playback.SeekTargets
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
    val bookmarks: List<Bookmark> = emptyList(),
    val bookmarksLoading: Boolean = false,
    val isReconnecting: Boolean = false,
    val error: String? = null,
)

val SPEED_PRESETS: List<Float> = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val speedStore: SpeedStore,
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
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundObserver)
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
                        } else {
                            stopSyncTimer()
                            // Flush one last sync on pause so server sees the
                            // current position even if the user stops in
                            // mid-interval.
                            syncOnce()
                            // Start polling so another client's progress
                            // shows up here without needing the user to
                            // background the app.
                            startPausedPoll()
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
            // Same book already loaded — don't rebuild the playlist, but do
            // pull the latest position from the server in case another client
            // advanced it while we were elsewhere.
            refreshFromServer(itemId)
            return
        }
        if (!controllerReady.value) {
            pendingLoad = itemId to coverUrl
            _state.update { it.copy(loading = true, itemId = itemId, coverUrl = coverUrl, error = null) }
            return
        }
        // Close any previous server session before opening a new one for a
        // different book so ABS doesn't hold a dangling session per device.
        sessionId?.let { previous ->
            sessionId = null
            stopSyncTimer()
            viewModelScope.launch { playbackRepository.closeSession(previous) }
        }
        _state.update { it.copy(loading = true, itemId = itemId, coverUrl = coverUrl, error = null) }
        viewModelScope.launch {
            runCatching { playbackRepository.openPlayback(itemId) }
                .onSuccess { prep ->
                    sessionId = prep.session.id
                    chapters = prep.session.chapters
                    tracks = prep.session.audioTracks
                    val ctl = controller
                    if (ctl == null) {
                        _state.update { it.copy(loading = false, error = "Player not ready.") }
                        return@onSuccess
                    }
                    ctl.setMediaItems(prep.mediaItems, false)
                    ctl.prepare()
                    seekToAbsoluteMs(prep.resumePositionMs)
                    val savedSpeed = speedStore.get(itemId) ?: 1.0f
                    ctl.setPlaybackSpeed(savedSpeed)
                    _state.update {
                        it.copy(
                            loading = false,
                            title = prep.session.displayTitle ?: "",
                            author = prep.session.displayAuthor ?: "",
                            durationSec = prep.session.duration.toLong(),
                            positionSec = prep.resumePositionMs / 1000,
                            chapterTitle = currentChapterTitle(prep.resumePositionMs / 1000.0),
                            isPlaying = ctl.isPlaying,
                            speed = savedSpeed,
                        )
                    }
                    // Land paused -> start polling so external client changes
                    // converge without requiring the user to press Play.
                    if (!ctl.isPlaying) startPausedPoll()
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, error = t.message ?: "Failed to open playback.") }
                }
        }
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
        // start from there instead of replaying.
        viewModelScope.launch {
            val serverSec = playbackRepository.fetchSavedPositionSec(id)
            val ctlNow = controller ?: return@launch
            if (serverSec != null) {
                val localSec = absolutePositionSec(ctlNow).toDouble()
                if (kotlin.math.abs(serverSec - localSec) > POSITION_DRIFT_TOLERANCE_SEC) {
                    seekToAbsoluteMs((serverSec * 1000).toLong())
                    _state.update {
                        it.copy(
                            positionSec = serverSec.toLong(),
                            chapterTitle = currentChapterTitle(serverSec),
                        )
                    }
                }
            }
            controller?.play()
        }
    }

    // Skip handlers obey the position-is-server-truth invariant: each
    // user-initiated seek must call pushPositionToServer so the pre-play
    // refresh in togglePlayPause doesn't snap us back. Any new seek path
    // added here MUST keep the pushPositionToServer call.
    fun skipBack30() {
        val ctl = controller ?: return
        val target = SeekTargets.skipBack(absolutePositionSec(ctl))
        seekToAbsoluteMs(target * 1000)
        pushPositionToServer(target.toDouble())
    }

    fun skipForward30() {
        val ctl = controller ?: return
        val target = SeekTargets.skipForward(absolutePositionSec(ctl), _state.value.durationSec)
        seekToAbsoluteMs(target * 1000)
        pushPositionToServer(target.toDouble())
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

    fun openBookmarkPanel() {
        val id = _state.value.itemId ?: return
        _state.update { it.copy(bookmarkPanelVisible = true, bookmarksLoading = true) }
        viewModelScope.launch {
            val items = bookmarksRepository.fetchForItem(id)
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
                val merged = (it.bookmarks + created).sortedBy { b -> b.timeSec }
                it.copy(bookmarks = merged)
            }
        }
    }

    fun jumpToBookmark(bookmark: Bookmark) {
        // Also a user-initiated seek -- push to server so togglePlayPause's
        // pre-play refresh doesn't pull us back to the prior position.
        seekToAbsoluteMs(bookmark.timeSec * 1000)
        _state.update {
            it.copy(
                positionSec = bookmark.timeSec,
                chapterTitle = currentChapterTitle(bookmark.timeSec.toDouble()),
            )
        }
        pushPositionToServer(bookmark.timeSec.toDouble())
    }

    /**
     * User-initiated seek: tell the server this is our new position so the
     * pre-play refresh in togglePlayPause doesn't snap us back to a stale
     * "where you last paused" value when the user hits Play next.
     */
    private fun pushPositionToServer(timeSec: Double) {
        val sid = sessionId ?: return
        val dur = _state.value.durationSec.toDouble()
        viewModelScope.launch {
            playbackRepository.syncProgress(
                sessionId = sid,
                currentTimeSec = timeSec,
                timeListenedSec = 0.0,
                durationSec = dur,
            )
        }
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
        // playing — yanking the head backward mid-listen is worse than a
        // small drift, and any active play is already syncing to ABS itself.
        val ctl = controller ?: return
        if (ctl.isPlaying) return
        viewModelScope.launch {
            val serverSec = playbackRepository.fetchSavedPositionSec(itemId) ?: return@launch
            val localSec = absolutePositionSec(ctl).toDouble()
            if (kotlin.math.abs(serverSec - localSec) > POSITION_DRIFT_TOLERANCE_SEC) {
                seekToAbsoluteMs((serverSec * 1000).toLong())
                _state.update {
                    it.copy(
                        positionSec = serverSec.toLong(),
                        chapterTitle = currentChapterTitle(serverSec),
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
        val currentSec = absolutePositionSec(ctl).toDouble()
        val now = System.currentTimeMillis()
        val deltaSec = if (lastSyncWallClockMs > 0) ((now - lastSyncWallClockMs) / 1000.0) else 0.0
        lastSyncWallClockMs = now
        val duration = _state.value.durationSec.toDouble()
        viewModelScope.launch {
            playbackRepository.syncProgress(
                sessionId = sid,
                currentTimeSec = currentSec,
                timeListenedSec = deltaSec.coerceAtLeast(0.0),
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
