package xyz.ludothegreat.audiobooktv.domain

import xyz.ludothegreat.audiobooktv.data.abs.AbsApiProvider
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsLibraryItem
import xyz.ludothegreat.audiobooktv.data.abs.dto.MediaProgress
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val apiProvider: AbsApiProvider,
    private val sessionManager: SessionManager,
) {
    /** Returns books for the first available library, merged with the user's progress. */
    suspend fun fetchBooks(): List<Book> {
        val api = apiProvider.get()
        val libraries = api.libraries().libraries
        val library = libraries.firstOrNull { it.mediaType == "book" }
            ?: libraries.firstOrNull()
            ?: return emptyList()

        val items = api.libraryItems(libraryId = library.id, limit = 0).results
        val progressByItem = api.me().mediaProgress
            .associateBy { it.libraryItemId ?: "" }
            .filterKeys { it.isNotEmpty() }

        val baseUrl = sessionManager.currentTarget()?.baseUrl?.trimEnd('/').orEmpty()
        return items.map { it.toBook(progressByItem[it.id], baseUrl) }
    }

    suspend fun fetchInProgress(): List<Book> {
        val api = apiProvider.get()
        val inProgress = api.itemsInProgress().libraryItems
        val progressByItem = api.me().mediaProgress
            .associateBy { it.libraryItemId ?: "" }
            .filterKeys { it.isNotEmpty() }
        val baseUrl = sessionManager.currentTarget()?.baseUrl?.trimEnd('/').orEmpty()
        return inProgress.map { it.toBook(progressByItem[it.id], baseUrl) }
            .sortedByDescending { it.lastUpdate }
    }

    private fun AbsLibraryItem.toBook(progress: MediaProgress?, baseUrl: String): Book {
        val metadata = media?.metadata
        return Book(
            id = id,
            title = metadata?.title ?: "Untitled",
            author = metadata?.authorName,
            series = metadata?.seriesName,
            coverUrl = if (baseUrl.isNotEmpty()) "$baseUrl/api/items/$id/cover" else null,
            durationSec = (media?.duration ?: 0.0).toLong(),
            numChapters = media?.numChapters ?: 0,
            progressFraction = progress?.progress ?: 0.0,
            isFinished = progress?.isFinished == true,
            lastUpdate = progress?.lastUpdate ?: 0L,
        )
    }
}
