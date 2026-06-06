package xyz.ludothegreat.audiobooktv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.ludothegreat.audiobooktv.domain.Book

/**
 * Locks decision #14's library ordering: most-recently-played in-progress
 * first, then never-played alphabetical by author, then finished last.
 * Future refactor that breaks this will be obvious from these tests.
 */
class LibrarySorterTest {

    private fun book(
        id: String,
        title: String,
        author: String? = null,
        lastUpdate: Long = 0,
        isFinished: Boolean = false,
    ) = Book(
        id = id,
        title = title,
        author = author,
        series = null,
        coverUrl = null,
        durationSec = 0,
        numChapters = 0,
        progressFraction = if (lastUpdate > 0) 0.5 else 0.0,
        isFinished = isFinished,
        lastUpdate = lastUpdate,
    )

    @Test
    fun `in-progress books come first sorted by most recent update`() {
        val older = book(id = "a", title = "Foo", lastUpdate = 100)
        val newer = book(id = "b", title = "Bar", lastUpdate = 200)
        val never = book(id = "c", title = "Baz")
        val result = LibrarySorter.sortForGrid(listOf(older, never, newer))
        assertEquals(listOf("b", "a", "c"), result.map { it.id })
    }

    @Test
    fun `never-played books sort alphabetically by author then title`() {
        val z = book(id = "z", title = "Aardvark", author = "Z. Author")
        val aTitle1 = book(id = "a1", title = "Bee", author = "A. Author")
        val aTitle2 = book(id = "a2", title = "Aphid", author = "A. Author")
        val result = LibrarySorter.sortForGrid(listOf(z, aTitle1, aTitle2))
        assertEquals(listOf("a2", "a1", "z"), result.map { it.id })
    }

    @Test
    fun `finished books sink below unfinished even when previously played`() {
        val inProgress = book(id = "ip", title = "Live", lastUpdate = 500)
        val finished = book(id = "fin", title = "Done", lastUpdate = 1000, isFinished = true)
        val unplayed = book(id = "new", title = "Apple")
        val result = LibrarySorter.sortForGrid(listOf(finished, unplayed, inProgress))
        assertEquals(listOf("ip", "new", "fin"), result.map { it.id })
    }

    @Test
    fun `null author treated as empty string for sort`() {
        val withAuthor = book(id = "a", title = "Z", author = "Aaron")
        val nullAuthor = book(id = "b", title = "Y", author = null)
        // Empty author sorts before "aaron" because "" < "aaron".
        val result = LibrarySorter.sortForGrid(listOf(withAuthor, nullAuthor))
        assertEquals(listOf("b", "a"), result.map { it.id })
    }

    @Test
    fun `sort is case insensitive on author and title`() {
        val a = book(id = "a", title = "z", author = "ALPHA")
        val b = book(id = "b", title = "A", author = "alpha")
        val result = LibrarySorter.sortForGrid(listOf(a, b))
        // Same lowercase author, so title decides: lowercase "a" < "z".
        assertEquals(listOf("b", "a"), result.map { it.id })
    }
}
