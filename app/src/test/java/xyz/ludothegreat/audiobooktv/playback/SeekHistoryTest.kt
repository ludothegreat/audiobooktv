package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the undo-seek contract: bounded history, newest-first undo,
 * undo-of-undo acting as redo, and suppression of records that carry
 * nothing to undo. The ViewModel trusts these semantics blindly, so a
 * change here is a change to what the Undo button does.
 */
class SeekHistoryTest {

    private fun history(capacity: Int = SeekHistory.DEFAULT_CAPACITY) = SeekHistory(capacity)

    @Test
    fun `capacity is capped at 10 and the oldest records are evicted`() {
        val subject = history()
        for (i in 1..12) {
            subject.record(fromSec = i * 100L, toSec = i * 100L + 50, cause = SeekCause.Skip, atMs = i.toLong())
        }
        assertEquals(10, subject.size)
        // Records 1 and 2 fell off the front; 3..12 remain oldest-to-newest.
        assertEquals(300L, subject.snapshot().first().fromSec)
        assertEquals(1200L, subject.snapshot().last().fromSec)
    }

    @Test
    fun `snapshot preserves insertion order oldest to newest`() {
        val subject = history()
        subject.record(fromSec = 10, toSec = 40, cause = SeekCause.Skip, atMs = 1)
        subject.record(fromSec = 40, toSec = 500, cause = SeekCause.Scrub, atMs = 2)
        subject.record(fromSec = 500, toSec = 730, cause = SeekCause.BookmarkJump, atMs = 3)
        assertEquals(
            listOf(SeekCause.Skip, SeekCause.Scrub, SeekCause.BookmarkJump),
            subject.snapshot().map { it.cause },
        )
    }

    @Test
    fun `peekUndo returns the newest record`() {
        val subject = history()
        subject.record(fromSec = 10, toSec = 40, cause = SeekCause.Skip, atMs = 1)
        subject.record(fromSec = 40, toSec = 500, cause = SeekCause.Scrub, atMs = 2)
        assertEquals(40L, subject.peekUndo()?.fromSec)
        assertEquals(500L, subject.peekUndo()?.toSec)
    }

    @Test
    fun `undo of undo offers redo`() {
        val subject = history()
        // User scrubs 100 -> 900, then presses Undo. The undo seek goes back
        // to 100 and is recorded through the same path as any other seek.
        subject.record(fromSec = 100, toSec = 900, cause = SeekCause.Scrub, atMs = 1)
        val undoTarget = subject.peekUndo()!!.fromSec
        assertEquals(100L, undoTarget)
        subject.record(fromSec = 900, toSec = undoTarget, cause = SeekCause.Undo, atMs = 2)
        // Pressing Undo again now offers the original destination back: redo.
        assertEquals(900L, subject.peekUndo()?.fromSec)
    }

    @Test
    fun `no-move seek is not recorded`() {
        val subject = history()
        assertFalse(subject.record(fromSec = 300, toSec = 300, cause = SeekCause.Scrub, atMs = 1))
        assertEquals(0, subject.size)
        assertNull(subject.peekUndo())
    }

    @Test
    fun `exact repeat of the newest record is suppressed`() {
        val subject = history()
        assertTrue(subject.record(fromSec = 100, toSec = 200, cause = SeekCause.Scrub, atMs = 1))
        // Same from, to, and cause: a double-fired control, not a new intent.
        assertFalse(subject.record(fromSec = 100, toSec = 200, cause = SeekCause.Scrub, atMs = 2))
        assertEquals(1, subject.size)
    }

    @Test
    fun `same jump with a different cause is a new record`() {
        val subject = history()
        subject.record(fromSec = 100, toSec = 200, cause = SeekCause.Scrub, atMs = 1)
        assertTrue(subject.record(fromSec = 100, toSec = 200, cause = SeekCause.BookmarkJump, atMs = 2))
        assertEquals(2, subject.size)
    }

    @Test
    fun `repeat that is not the newest record is recorded again`() {
        val subject = history()
        subject.record(fromSec = 100, toSec = 200, cause = SeekCause.Scrub, atMs = 1)
        subject.record(fromSec = 200, toSec = 50, cause = SeekCause.Skip, atMs = 2)
        // 100 -> 200 again is a genuine new user intent now.
        assertTrue(subject.record(fromSec = 100, toSec = 200, cause = SeekCause.Scrub, atMs = 3))
        assertEquals(3, subject.size)
    }

    @Test
    fun `clear empties the history`() {
        val subject = history()
        subject.record(fromSec = 10, toSec = 40, cause = SeekCause.Skip, atMs = 1)
        subject.clear()
        assertEquals(0, subject.size)
        assertNull(subject.peekUndo())
    }

    @Test
    fun `capacity must be positive`() {
        assertThrows(IllegalArgumentException::class.java) { SeekHistory(0) }
        assertThrows(IllegalArgumentException::class.java) { SeekHistory(-3) }
    }
}
