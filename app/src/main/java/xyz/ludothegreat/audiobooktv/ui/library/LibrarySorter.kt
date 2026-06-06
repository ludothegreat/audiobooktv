package xyz.ludothegreat.audiobooktv.ui.library

import xyz.ludothegreat.audiobooktv.domain.Book

/**
 * Library grid ordering rule (decision #14):
 * 1. In-progress books first, most-recently-played at the top.
 * 2. Then never-played books, alphabetical by author then title.
 * 3. Finished books last (kept in the list because re-listen is common),
 *    interleaved with never-played by the same author/title order.
 *
 * The sort runs on every refresh — keep it allocation-light and stable.
 */
internal object LibrarySorter {
    fun sortForGrid(books: List<Book>): List<Book> {
        val inProgress = books.filter { it.lastUpdate > 0 && !it.isFinished }
            .sortedByDescending { it.lastUpdate }
        val others = books.filter { it.lastUpdate == 0L || it.isFinished }
            .sortedWith(
                compareBy(
                    { it.isFinished },
                    { (it.author ?: "").lowercase() },
                    { it.title.lowercase() },
                ),
            )
        return inProgress + others
    }
}
