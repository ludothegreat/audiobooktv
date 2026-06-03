package xyz.ludothegreat.audiobooktv.data.abs.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PingResponse(val success: Boolean? = null)

@Serializable
data class StatusResponse(
    @SerialName("serverVersion") val serverVersion: String? = null,
    @SerialName("isInit") val isInit: Boolean? = null,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(
    val user: AbsUser? = null,
    val error: String? = null,
)

@Serializable
data class AbsUser(
    val id: String,
    val username: String,
    val token: String? = null,
    val type: String? = null,
)

@Serializable
data class LibrariesResponse(val libraries: List<AbsLibrary> = emptyList())

@Serializable
data class AbsLibrary(
    val id: String,
    val name: String,
    val mediaType: String? = null,
)

@Serializable
data class LibraryItemsResponse(
    val results: List<AbsLibraryItem> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val page: Int = 0,
)

@Serializable
data class AbsLibraryItem(
    val id: String,
    val libraryId: String? = null,
    val mediaType: String? = null,
    val media: AbsMedia? = null,
    val updatedAt: Long? = null,
    val addedAt: Long? = null,
)

@Serializable
data class AbsMedia(
    val id: String? = null,
    val metadata: AbsMetadata? = null,
    val coverPath: String? = null,
    val duration: Double? = null,
    val numChapters: Int? = null,
    val numTracks: Int? = null,
)

@Serializable
data class AbsMetadata(
    val title: String? = null,
    val subtitle: String? = null,
    val authorName: String? = null,
    val narratorName: String? = null,
    val seriesName: String? = null,
    val publishedYear: String? = null,
    val description: String? = null,
)

@Serializable
data class ItemsInProgressResponse(
    val libraryItems: List<AbsLibraryItem> = emptyList(),
)

@Serializable
data class MediaProgress(
    val id: String? = null,
    val libraryItemId: String? = null,
    val episodeId: String? = null,
    val duration: Double? = null,
    val progress: Double? = null,
    val currentTime: Double? = null,
    val isFinished: Boolean? = null,
    val hideFromContinueListening: Boolean? = null,
    val lastUpdate: Long? = null,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
)

@Serializable
data class MeResponse(
    val id: String? = null,
    val username: String? = null,
    val mediaProgress: List<MediaProgress> = emptyList(),
)

@Serializable
data class PlayRequest(
    val deviceInfo: DeviceInfo,
    val supportedMimeTypes: List<String>,
    val forceTranscode: Boolean = false,
    val forceDirectPlay: Boolean = false,
    val mediaPlayer: String = "audiobooktv",
)

@Serializable
data class DeviceInfo(
    val clientName: String,
    val clientVersion: String,
    val deviceName: String,
    val manufacturer: String? = null,
    val model: String? = null,
    val sdkVersion: Int? = null,
)

@Serializable
data class PlaybackSession(
    val id: String,
    val libraryItemId: String? = null,
    val displayTitle: String? = null,
    val displayAuthor: String? = null,
    val coverPath: String? = null,
    val duration: Double = 0.0,
    val currentTime: Double = 0.0,
    val playMethod: Int = 0,
    val chapters: List<AbsChapter> = emptyList(),
    val audioTracks: List<AbsAudioTrack> = emptyList(),
)

@Serializable
data class AbsChapter(
    val id: Int? = null,
    val start: Double = 0.0,
    val end: Double = 0.0,
    val title: String? = null,
)

@Serializable
data class AbsAudioTrack(
    val index: Int = 0,
    val ino: String? = null,
    val title: String? = null,
    val mimeType: String? = null,
    val codec: String? = null,
    val duration: Double = 0.0,
    val startOffset: Double = 0.0,
    val contentUrl: String,
)

@Serializable
data class SessionSyncRequest(
    val currentTime: Double,
    val timeListened: Double,
    val duration: Double,
)
