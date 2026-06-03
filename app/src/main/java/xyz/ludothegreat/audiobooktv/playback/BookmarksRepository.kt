package xyz.ludothegreat.audiobooktv.playback

import xyz.ludothegreat.audiobooktv.data.abs.AbsApiProvider
import xyz.ludothegreat.audiobooktv.data.abs.dto.CreateBookmarkRequest
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarksRepository @Inject constructor(
    private val apiProvider: AbsApiProvider,
) {
    suspend fun fetchForItem(itemId: String): List<Bookmark> = runCatching {
        apiProvider.get().me().bookmarks
            .filter { it.libraryItemId == itemId }
            .sortedBy { it.time }
            .map { Bookmark(timeSec = it.time.toLong(), title = it.title.orEmpty()) }
    }.getOrDefault(emptyList())

    suspend fun create(itemId: String, timeSec: Long, title: String): Bookmark? = runCatching {
        val created = apiProvider.get().createBookmark(
            itemId = itemId,
            body = CreateBookmarkRequest(time = timeSec.toDouble(), title = title),
        )
        Bookmark(timeSec = created.time.toLong(), title = created.title.orEmpty())
    }.getOrNull()
}
