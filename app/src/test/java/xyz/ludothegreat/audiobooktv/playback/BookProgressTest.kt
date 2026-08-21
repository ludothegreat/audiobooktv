package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the book-level percent and speed-aware countdown rules that drive
 * the right endpoint of the book bar on both surfaces. The floor/ceil
 * pairing is the contract: percent must not claim 100 early, and the
 * countdown must not reach 0:00 before the book actually ends.
 */
class BookProgressTest {

    @Test
    fun `percent floors instead of rounding up`() {
        // 999/1000 = 99.9%, must display 99, not 100.
        assertEquals(99, BookProgress.percent(positionSec = 999, durationSec = 1000))
    }

    @Test
    fun `percent reaches 100 only at the true end`() {
        assertEquals(100, BookProgress.percent(positionSec = 1000, durationSec = 1000))
        assertEquals(0, BookProgress.percent(positionSec = 0, durationSec = 1000))
        assertEquals(50, BookProgress.percent(positionSec = 500, durationSec = 1000))
    }

    @Test
    fun `percent clamps a position past the duration`() {
        assertEquals(100, BookProgress.percent(positionSec = 1500, durationSec = 1000))
    }

    @Test
    fun `unknown duration reads as zero percent`() {
        assertEquals(0, BookProgress.percent(positionSec = 500, durationSec = 0))
        assertEquals(0, BookProgress.percent(positionSec = 500, durationSec = -1))
    }

    @Test
    fun `remaining at 1x is plain content remaining`() {
        assertEquals(400, BookProgress.remainingSecAtSpeed(positionSec = 600, durationSec = 1000, speed = 1.0f))
    }

    @Test
    fun `remaining is divided by the playback rate`() {
        assertEquals(200, BookProgress.remainingSecAtSpeed(positionSec = 600, durationSec = 1000, speed = 2.0f))
        assertEquals(800, BookProgress.remainingSecAtSpeed(positionSec = 600, durationSec = 1000, speed = 0.5f))
    }

    @Test
    fun `remaining ceils so the countdown never hits zero early`() {
        // 100 content-seconds at 1.5x = 66.67 wall seconds -> 67, not 66.
        assertEquals(67, BookProgress.remainingSecAtSpeed(positionSec = 900, durationSec = 1000, speed = 1.5f))
    }

    @Test
    fun `non-positive rate falls back to 1x`() {
        assertEquals(400, BookProgress.remainingSecAtSpeed(positionSec = 600, durationSec = 1000, speed = 0f))
        assertEquals(400, BookProgress.remainingSecAtSpeed(positionSec = 600, durationSec = 1000, speed = -1f))
    }

    @Test
    fun `position past the end and unknown duration both read as zero remaining`() {
        assertEquals(0, BookProgress.remainingSecAtSpeed(positionSec = 1500, durationSec = 1000, speed = 1.0f))
        assertEquals(0, BookProgress.remainingSecAtSpeed(positionSec = 500, durationSec = 0, speed = 1.0f))
    }
}
