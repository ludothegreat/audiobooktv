package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
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
    fun `earliest inserted wins on a tie`() {
        val normalized = BookmarkList.normalize(
            listOf(
                Bookmark(timeSec = 90, title = "kept"),
                Bookmark(timeSec = 90, title = "dropped"),
            ),
        )
        assertEquals("kept", normalized.single().title)
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<Bookmark>(), BookmarkList.normalize(emptyList()))
    }
}
