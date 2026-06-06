package xyz.ludothegreat.audiobooktv.ui.library

import app.cash.turbine.test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.ludothegreat.audiobooktv.data.cache.LibraryCacheStorage
import xyz.ludothegreat.audiobooktv.data.cache.LibraryRefreshBus
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.domain.LibraryBookSource
import xyz.ludothegreat.audiobooktv.testutil.MainDispatcherRule
import java.io.IOException

/**
 * Locks decision #17's offline behaviour: when the live fetch fails, the
 * UI falls back to whatever the LibraryCache last knew and surfaces the
 * "offline" badge. With no cache, the user sees an error message instead.
 */
class LibraryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun `successful fetch writes cache and clears offline state`() = runTest {
        val repo = FakeLibrarySource(books = listOf(book("a"), book("b")))
        val cache = FakeLibraryCacheStorage(initial = emptyList())
        val vm = LibraryViewModel(repo, cache, LibraryRefreshBus())
        advanceUntilIdle()

        vm.state.test {
            val state = awaitItem()
            assertFalse("expected loading=false after fetch", state.loading)
            assertFalse("expected offline=false on happy path", state.offline)
            assertEquals(2, state.books.size)
        }
        assertEquals("cache should be written with fresh data", 2, cache.snapshot().size)
    }

    @Test
    fun `network failure falls back to cached books and flips offline=true`() = runTest {
        val cached = listOf(book("cached"))
        val repo = FakeLibrarySource(throws = IOException("no internet"))
        val cache = FakeLibraryCacheStorage(initial = cached)
        val vm = LibraryViewModel(repo, cache, LibraryRefreshBus())
        advanceUntilIdle()

        vm.state.test {
            val state = awaitItem()
            assertEquals(1, state.books.size)
            assertEquals("cached", state.books[0].id)
            assertTrue("expected offline=true on fetch failure with cache", state.offline)
        }
    }

    @Test
    fun `network failure with empty cache surfaces an error message`() = runTest {
        val repo = FakeLibrarySource(throws = IOException("no internet"))
        val cache = FakeLibraryCacheStorage(initial = emptyList())
        val vm = LibraryViewModel(repo, cache, LibraryRefreshBus())
        advanceUntilIdle()

        vm.state.test {
            val state = awaitItem()
            assertEquals(0, state.books.size)
            assertTrue(state.offline)
            assertNotNull(state.error)
        }
    }

    @Test
    fun `refreshBus event re-fetches and clears offline once it recovers`() = runTest {
        val repo = FakeLibrarySource(throws = IOException("flaky"))
        val cache = FakeLibraryCacheStorage(initial = listOf(book("cached")))
        val bus = LibraryRefreshBus()
        val vm = LibraryViewModel(repo, cache, bus)
        advanceUntilIdle()

        // Initial state: offline with the cached book.
        assertTrue(vm.state.value.offline)
        assertEquals(1, vm.state.value.books.size)

        // Recover and tell the bus to refresh.
        repo.swap(books = listOf(book("live-1"), book("live-2")))
        bus.request()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.offline)
        assertEquals(2, state.books.size)
    }

    private fun book(id: String) = Book(
        id = id,
        title = "Title $id",
        author = "Author",
        series = null,
        coverUrl = null,
        durationSec = 0,
        numChapters = 0,
        progressFraction = 0.0,
        isFinished = false,
        lastUpdate = 0,
    )

    private class FakeLibrarySource(
        private var books: List<Book> = emptyList(),
        private var throws: Throwable? = null,
    ) : LibraryBookSource {
        fun swap(books: List<Book>, throws: Throwable? = null) {
            this.books = books
            this.throws = throws
        }
        override suspend fun fetchBooks(): List<Book> {
            throws?.let { throw it }
            return books
        }
        override suspend fun fetchInProgress(): List<Book> = books
    }

    private class FakeLibraryCacheStorage(initial: List<Book>) : LibraryCacheStorage {
        private var stored: List<Book> = initial
        fun snapshot(): List<Book> = stored
        override suspend fun read(): List<Book> = stored
        override suspend fun write(books: List<Book>) {
            stored = books
        }
        override suspend fun clear() {
            stored = emptyList()
        }
    }
}
