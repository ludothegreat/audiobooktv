package xyz.ludothegreat.audiobooktv.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsAudioTrack
import xyz.ludothegreat.audiobooktv.playback.PlaybackRepository
import xyz.ludothegreat.audiobooktv.playback.PlayerService
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
    val error: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val controllerReady = MutableStateFlow(false)
    private var chapters: List<AbsChapter> = emptyList()
    private var tracks: List<AbsAudioTrack> = emptyList()
    private var sessionId: String? = null
    private var pendingLoad: Pair<String, String?>? = null

    init {
        bindController()
        startTicker()
    }

    private fun bindController() {
        val token = SessionToken(context, ComponentName(context, PlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { ctl ->
                ctl.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.update { it.copy(isPlaying = isPlaying) }
                    }
                })
                _state.update { it.copy(isPlaying = ctl.isPlaying) }
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
                delay(250)
                val ctl = controller ?: continue
                if (ctl.duration <= 0) continue
                val absSec = absolutePositionSec(ctl)
                _state.update {
                    it.copy(
                        positionSec = absSec,
                        chapterTitle = currentChapterTitle(absSec.toDouble()),
                    )
                }
            }
        }
    }

    fun load(itemId: String, coverUrl: String?) {
        if (_state.value.itemId == itemId && (controller?.mediaItemCount ?: 0) > 0) return
        if (!controllerReady.value) {
            pendingLoad = itemId to coverUrl
            _state.update { it.copy(loading = true, itemId = itemId, coverUrl = coverUrl, error = null) }
            return
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
                    _state.update {
                        it.copy(
                            loading = false,
                            title = prep.session.displayTitle ?: "",
                            author = prep.session.displayAuthor ?: "",
                            durationSec = prep.session.duration.toLong(),
                            positionSec = prep.resumePositionMs / 1000,
                            chapterTitle = currentChapterTitle(prep.resumePositionMs / 1000.0),
                            isPlaying = ctl.isPlaying,
                        )
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, error = t.message ?: "Failed to open playback.") }
                }
        }
    }

    fun togglePlayPause() {
        val ctl = controller ?: return
        if (ctl.isPlaying) ctl.pause() else ctl.play()
    }

    fun skipBack30() {
        val ctl = controller ?: return
        val target = (absolutePositionSec(ctl) - 30).coerceAtLeast(0)
        seekToAbsoluteMs(target * 1000)
    }

    fun skipForward30() {
        val ctl = controller ?: return
        val total = _state.value.durationSec
        val target = (absolutePositionSec(ctl) + 30).coerceAtMost(total)
        seekToAbsoluteMs(target * 1000)
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    private fun absolutePositionSec(ctl: MediaController): Long {
        val trackOffsetSec = tracks.getOrNull(ctl.currentMediaItemIndex)?.startOffset ?: 0.0
        return ((ctl.currentPosition / 1000.0) + trackOffsetSec).toLong()
    }

    private fun seekToAbsoluteMs(absoluteMs: Long) {
        val ctl = controller ?: return
        if (tracks.isEmpty()) {
            ctl.seekTo(absoluteMs)
            return
        }
        val absoluteSec = absoluteMs / 1000.0
        val track = tracks.indexOfLast { it.startOffset <= absoluteSec }.coerceAtLeast(0)
        val withinTrackSec = absoluteSec - (tracks[track].startOffset)
        ctl.seekTo(track, (withinTrackSec * 1000).toLong().coerceAtLeast(0))
    }

    private fun currentChapterTitle(absSec: Double): String {
        val c = chapters.firstOrNull { absSec >= it.start && absSec < it.end }
        return c?.title.orEmpty()
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}
