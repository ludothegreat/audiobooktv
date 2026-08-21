package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the print-the-time-once rule for bookmark rows. Rows always lead
 * with formatTimestampHms(timeSec); the secondary label must vanish
 * exactly when the title is that same timestamp (the addBookmarkHere
 * default) and must never touch a real custom title.
 */
class BookmarkLabelTest {

    @Test
    fun `default title equal to its own timestamp prints once`() {
        // 5025s formats as 1:23:45, the exact string addBookmarkHere writes.
        assertEquals("1:23:45", formatTimestampHms(5025))
        assertNull(BookmarkLabel.secondary("1:23:45", 5025, "-"))
    }

    @Test
    fun `sub-hour default collapses too`() {
        // 754s formats as 12:34 (the MM:SS branch of formatTimestampHms).
        assertEquals("12:34", formatTimestampHms(754))
        assertNull(BookmarkLabel.secondary("12:34", 754, "-"))
    }

    @Test
    fun `padded default still collapses`() {
        assertNull(BookmarkLabel.secondary("  1:23:45 ", 5025, "-"))
    }

    @Test
    fun `custom title is returned untouched`() {
        assertEquals(
            "The dragon reveal",
            BookmarkLabel.secondary("The dragon reveal", 5025, "-"),
        )
    }

    @Test
    fun `title matching a DIFFERENT time's format is kept`() {
        // A rename to "1:23:45" on a bookmark at 0:30 is a deliberate label,
        // not a duplicate; only the row's own timestamp collapses.
        assertEquals("1:23:45", BookmarkLabel.secondary("1:23:45", 30, "-"))
    }

    @Test
    fun `blank title falls back to the surface's placeholder`() {
        assertEquals("-", BookmarkLabel.secondary("", 5025, "-"))
        assertEquals("(no label)", BookmarkLabel.secondary("   ", 5025, "(no label)"))
    }
}
