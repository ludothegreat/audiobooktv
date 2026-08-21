package xyz.ludothegreat.audiobooktv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.ludothegreat.audiobooktv.domain.Book

/**
 * Locks the client-side library search + status segmentation. Everything
 * here runs over the already-cached list: no API, no Android types.
 */
class LibraryFilterTest {

    private fun book(
        id: String,
        title: String = "Title $id",
        author: String? = null,
        series: String? = null,
        progress: Double = 0.0,
        isFinished: Boolean = false,
        lastUpdate: Long = 0,
    ) = Book(
        id = id,
        title = title,
        author = author,
        series = series,
        coverUrl = null,
        durationSec = 0,
        numChapters = 0,
        progressFraction = progress,
        isFinished = isFinished,
        lastUpdate = lastUpdate,
    )

    // ---- status derivation ----

    @Test
    fun `no progress derives NEW`() {
        assertEquals(StatusSegment.NEW, LibraryFilter.statusOf(book("a", progress = 0.0)))
    }

    @Test
    fun `zero progress with a lastUpdate is still NEW`() {
        // Opening a book writes a mediaProgress row before any listening.
        val opened = book("a", progress = 0.0, lastUpdate = 12345)
        assertEquals(StatusSegment.NEW, LibraryFilter.statusOf(opened))
    }

    @Test
    fun `partial progress derives STARTED`() {
        assertEquals(StatusSegment.STARTED, LibraryFilter.statusOf(book("a", progress = 0.001)))
        assertEquals(StatusSegment.STARTED, LibraryFilter.statusOf(book("b", progress = 0.5)))
        assertEquals(StatusSegment.STARTED, LibraryFilter.statusOf(book("c", progress = 0.999)))
    }

    @Test
    fun `isFinished flag derives FINISHED regardless of fraction`() {
        assertEquals(
            StatusSegment.FINISHED,
            LibraryFilter.statusOf(book("a", progress = 0.4, isFinished = true)),
        )
    }

    @Test
    fun `progress at or past 1 derives FINISHED even without the flag`() {
        assertEquals(StatusSegment.FINISHED, LibraryFilter.statusOf(book("a", progress = 1.0)))
        assertEquals(StatusSegment.FINISHED, LibraryFilter.statusOf(book("b", progress = 1.02)))
    }

    // ---- search matching ----

    @Test
    fun `blank query matches everything`() {
        assertTrue(LibraryFilter.matches(book("a", title = "Anything"), ""))
        assertTrue(LibraryFilter.matches(book("a", title = "Anything"), "   "))
    }

    @Test
    fun `title match is case-insensitive substring`() {
        val b = book("a", title = "The Long Way to a Small Angry Planet")
        assertTrue(LibraryFilter.matches(b, "angry"))
        assertTrue(LibraryFilter.matches(b, "SMALL ANGRY"))
        assertFalse(LibraryFilter.matches(b, "wayfarer"))
    }

    @Test
    fun `author and series fields match too`() {
        val b = book("a", title = "Book", author = "Becky Chambers", series = "Wayfarers #1")
        assertTrue(LibraryFilter.matches(b, "chambers"))
        assertTrue(LibraryFilter.matches(b, "wayfarers"))
    }

    @Test
    fun `null author and series never match but never crash`() {
        val b = book("a", title = "Solo", author = null, series = null)
        assertFalse(LibraryFilter.matches(b, "someone"))
        assertTrue(LibraryFilter.matches(b, "solo"))
    }

    @Test
    fun `accented field matches plain query`() {
        val b = book("a", title = "Amélie Nothomb: Stupeur", author = "Amélie Nothomb")
        assertTrue(LibraryFilter.matches(b, "amelie"))
    }

    @Test
    fun `accented query matches plain field`() {
        val b = book("a", title = "Amelie")
        assertTrue(LibraryFilter.matches(b, "Amélie"))
    }

    @Test
    fun `non-decomposable letters fold to ascii`() {
        assertTrue(LibraryFilter.matches(book("a", title = "Sløborn"), "sloborn"))
        assertTrue(LibraryFilter.matches(book("b", title = "Straße der Toten"), "strasse"))
        assertTrue(LibraryFilter.matches(book("c", author = "Łukasz"), "lukasz"))
    }

    @Test
    fun `query with surrounding whitespace is trimmed`() {
        assertTrue(LibraryFilter.matches(book("a", title = "Dune"), "  dune  "))
    }

    // ---- apply: segment + query + order stability ----

    private fun sortedFixture(): List<Book> = listOf(
        // Order as LibrarySorter would emit: in-progress newest first,
        // then never-played alphabetical, finished last.
        book("s2", title = "Zeta Started", progress = 0.6, lastUpdate = 300),
        book("s1", title = "Alpha Started", progress = 0.2, lastUpdate = 200),
        book("n1", title = "Beta New"),
        book("n2", title = "Gamma New"),
        book("f1", title = "Delta Done", progress = 1.0, isFinished = true, lastUpdate = 100),
        book("f2", title = "Beta Done", progress = 0.9, isFinished = true, lastUpdate = 50),
    )

    @Test
    fun `ALL with blank query returns the list untouched`() {
        val books = sortedFixture()
        assertEquals(books, LibraryFilter.apply(books, "", StatusSegment.ALL))
    }

    @Test
    fun `each segment keeps the incoming order within it`() {
        val books = sortedFixture()
        assertEquals(
            listOf("s2", "s1"),
            LibraryFilter.apply(books, "", StatusSegment.STARTED).map { it.id },
        )
        assertEquals(
            listOf("n1", "n2"),
            LibraryFilter.apply(books, "", StatusSegment.NEW).map { it.id },
        )
        assertEquals(
            listOf("f1", "f2"),
            LibraryFilter.apply(books, "", StatusSegment.FINISHED).map { it.id },
        )
    }

    @Test
    fun `query and segment combine`() {
        val books = sortedFixture()
        assertEquals(
            listOf("f2"),
            LibraryFilter.apply(books, "beta", StatusSegment.FINISHED).map { it.id },
        )
        assertEquals(
            listOf("n1"),
            LibraryFilter.apply(books, "beta", StatusSegment.NEW).map { it.id },
        )
    }

    @Test
    fun `query across segments keeps global order`() {
        val books = sortedFixture()
        assertEquals(
            listOf("n1", "f2"),
            LibraryFilter.apply(books, "beta", StatusSegment.ALL).map { it.id },
        )
    }

    @Test
    fun `no matches yields an empty list not an error`() {
        assertEquals(
            emptyList<Book>(),
            LibraryFilter.apply(sortedFixture(), "zzz-nope", StatusSegment.ALL),
        )
    }

    // ---- empty-state copy ----

    @Test
    fun `empty line for a search names the query`() {
        assertEquals(
            "No matches for \"dune\".",
            LibraryFilter.emptyLine("dune", StatusSegment.ALL),
        )
        assertEquals(
            "No matches for \"dune\" in Finished.",
            LibraryFilter.emptyLine(" dune ", StatusSegment.FINISHED),
        )
    }

    @Test
    fun `empty line without a search names the segment`() {
        assertEquals(
            "No new books. Everything here has been started.",
            LibraryFilter.emptyLine("", StatusSegment.NEW),
        )
        assertEquals(
            "Nothing in progress. Pick a book to start listening.",
            LibraryFilter.emptyLine("", StatusSegment.STARTED),
        )
        assertEquals(
            "No finished books yet.",
            LibraryFilter.emptyLine("", StatusSegment.FINISHED),
        )
    }
}
