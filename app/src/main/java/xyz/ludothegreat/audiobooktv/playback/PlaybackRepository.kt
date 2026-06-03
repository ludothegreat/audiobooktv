package xyz.ludothegreat.audiobooktv.playback

import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import xyz.ludothegreat.audiobooktv.data.abs.AbsApiProvider
import xyz.ludothegreat.audiobooktv.data.abs.dto.DeviceInfo
import xyz.ludothegreat.audiobooktv.data.abs.dto.PlayRequest
import xyz.ludothegreat.audiobooktv.data.abs.dto.PlaybackSession
import xyz.ludothegreat.audiobooktv.data.abs.dto.SessionSyncRequest
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackPrep(
    val session: PlaybackSession,
    val mediaItems: List<MediaItem>,
    val resumePositionMs: Long,
)

@Singleton
class PlaybackRepository @Inject constructor(
    private val apiProvider: AbsApiProvider,
    private val sessionManager: SessionManager,
) {
    suspend fun openPlayback(itemId: String): PlaybackPrep {
        val api = apiProvider.get()
        val baseUrl = sessionManager.currentTarget()?.baseUrl?.trimEnd('/').orEmpty()
        val req = PlayRequest(
            deviceInfo = DeviceInfo(
                clientName = "audiobooktv",
                clientVersion = "0.1.0",
                deviceName = Build.MODEL ?: "AndroidTV",
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                sdkVersion = Build.VERSION.SDK_INT,
            ),
            supportedMimeTypes = SUPPORTED_MIME_TYPES,
        )
        val playback = api.openPlayback(itemId, req)
        val mediaItems = playback.audioTracks.mapIndexed { idx, track ->
            val url = if (track.contentUrl.startsWith("http")) {
                track.contentUrl
            } else {
                "$baseUrl${track.contentUrl}"
            }
            MediaItem.Builder()
                .setUri(url)
                .setMediaId("${playback.id}#$idx")
                .setMimeType(track.mimeType)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(playback.displayTitle ?: track.title)
                        .setArtist(playback.displayAuthor)
                        .build(),
                )
                .build()
        }
        val resumeMs = (playback.currentTime * 1000.0).toLong()
        return PlaybackPrep(session = playback, mediaItems = mediaItems, resumePositionMs = resumeMs)
    }

    suspend fun syncProgress(sessionId: String, currentTimeSec: Double, timeListenedSec: Double, durationSec: Double) {
        runCatching {
            apiProvider.get().syncSession(
                sessionId,
                SessionSyncRequest(
                    currentTime = currentTimeSec,
                    timeListened = timeListenedSec,
                    duration = durationSec,
                ),
            )
        }
    }

    suspend fun closeSession(sessionId: String) {
        runCatching { apiProvider.get().closeSession(sessionId) }
    }

    companion object {
        private val SUPPORTED_MIME_TYPES = listOf(
            "audio/mp4",
            "audio/mpeg",
            "audio/aac",
            "audio/flac",
            "audio/ogg",
            "audio/wav",
        )
    }
}
