package xyz.ludothegreat.audiobooktv.playback

import retrofit2.HttpException
import xyz.ludothegreat.audiobooktv.data.abs.AbsApiProvider
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsBookmark
import xyz.ludothegreat.audiobooktv.data.abs.dto.CreateBookmarkRequest
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import java.net.HttpURLConnection
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
            .map { it.toDomain() }
    }.getOrDefault(emptyList())

    suspend fun create(itemId: String, timeSec: Long, title: String): Bookmark? = runCatching {
        apiProvider.get().createBookmark(
            itemId = itemId,
            body = CreateBookmarkRequest(time = timeSec.toDouble(), title = title),
        ).toDomain()
    }.getOrNull()

    /**
     * Rename keeps Result instead of the null-on-failure shape above: the
     * caller must distinguish failure so it can leave state untouched and
     * tell the user loudly, not quietly render nothing.
     */
    suspend fun rename(itemId: String, timeSec: Long, title: String): Result<Bookmark> = runCatching {
        apiProvider.get().updateBookmark(
            itemId = itemId,
            body = CreateBookmarkRequest(time = timeSec.toDouble(), title = title),
        ).toDomain()
    }

    suspend fun delete(itemId: String, timeSec: Long): Result<Unit> = runCatching {
        apiProvider.get().deleteBookmark(itemId = itemId, timeSec = timeSec)
    }.recoverCatching { t ->
        // 404 means the bookmark is already gone server-side (deleted from
        // another client). The user's intent is satisfied, so removing it
        // locally is more correct than reporting a failure that would leave
        // a ghost entry until the next fetch.
        if (t is HttpException && t.code() == HttpURLConnection.HTTP_NOT_FOUND) Unit else throw t
    }

    private fun AbsBookmark.toDomain() = Bookmark(
        id = createdAt.takeIf { it > 0 }?.toString(),
        timeSec = time.toLong(),
        title = title.orEmpty(),
    )
}
