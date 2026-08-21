package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.ludothegreat.audiobooktv.domain.Bookmark

class BookmarkListTest {

    @Test
    fun `duplicate timestamps collapse to one entry`() {
        // The touch bookmark sheet keys its LazyColumn on timeSec. Two entries
        // at the same second throw "Key was already used" and kill the sheet,
        // so state must never hold them.
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(timeSec = 90, title = "00:01:30"),
                Bookmark(timeSec = 90, title = "00:01:30"),
            ),
        )
        assertEquals(1, normalized.size)
    }

    @Test
    fun `timestamps are unique after normalizing a mixed list`() {
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(timeSec = 300, title = "c"),
                Bookmark(timeSec = 90, title = "a"),
                Bookmark(timeSec = 300, title = "duplicate from another client"),
                Bookmark(timeSec = 90, title = "duplicate from a double tap"),
            ),
        )
        assertEquals(normalized.map { it.timeSec }.distinct(), normalized.map { it.timeSec })
    }

    @Test
    fun `result is sorted by timestamp`() {
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(timeSec = 300, title = "c"),
                Bookmark(timeSec = 30, title = "a"),
                Bookmark(timeSec = 120, title = "b"),
            ),
        )
        assertEquals(listOf(30L, 120L, 300L), normalized.map { it.timeSec })
    }

    @Test
    fun `latest entry wins on a timestamp tie`() {
        // Merges append the fresher server response after the cached list
        // (create and rename both do), so the later entry is server truth.
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(timeSec = 90, title = "stale"),
                Bookmark(timeSec = 90, title = "kept"),
            ),
        )
        assertEquals("kept", normalized.single().title)
    }

    @Test
    fun `same id collapses to the later entry`() {
        // A rename returns the same bookmark (same createdAt id, same time)
        // with a new title; the renamed copy must replace the original.
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(id = "1780526584570", timeSec = 130, title = "old label"),
                Bookmark(id = "1780526584570", timeSec = 130, title = "new label"),
            ),
        )
        assertEquals(1, normalized.size)
        assertEquals("new label", normalized.single().title)
    }

    @Test
    fun `different ids at the same time still collapse`() {
        // ABS upserts bookmarks by time, so two ids at one timestamp can only
        // be a stale local copy plus its server replacement. Keeping both
        // would also break the LazyColumn timeSec key.
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(id = "1780526584570", timeSec = 130, title = "stale local copy"),
                Bookmark(id = "1787288043916", timeSec = 130, title = "server replacement"),
            ),
        )
        assertEquals(1, normalized.size)
        assertEquals("server replacement", normalized.single().title)
    }

    @Test
    fun `missing id falls back to time identity`() {
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(id = null, timeSec = 130, title = "optimistic local"),
                Bookmark(id = "1787288043916", timeSec = 130, title = "from server"),
            ),
        )
        assertEquals(1, normalized.size)
        assertEquals("from server", normalized.single().title)
    }

    @Test
    fun `distinct ids at distinct times all survive`() {
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(id = "a", timeSec = 30, title = "one"),
                Bookmark(id = "b", timeSec = 120, title = "two"),
                Bookmark(id = "c", timeSec = 300, title = "three"),
            ),
        )
        assertEquals(3, normalized.size)
    }

    @Test
    fun `sameBookmark matches on id and falls back to time`() {
        val a = Bookmark(id = "x", timeSec = 10, title = "")
        assertTrue(BookmarkList.sameBookmark(a, Bookmark(id = "x", timeSec = 10, title = "renamed")))
        assertTrue(BookmarkList.sameBookmark(a, Bookmark(id = null, timeSec = 10, title = "")))
        assertFalse(BookmarkList.sameBookmark(a, Bookmark(id = "y", timeSec = 20, title = "")))
        assertFalse(
            BookmarkList.sameBookmark(
                Bookmark(id = null, timeSec = 10, title = ""),
                Bookmark(id = null, timeSec = 20, title = ""),
            ),
        )
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<Bookmark>(), BookmarkList.normalize(emptyList()))
    }
}
