package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the skip-30s clamp boundaries. A future refactor that turned 30
 * into a setting must keep the clamp -- the player must not be able to
 * seek past 0 or beyond the end of the book.
 */
class SeekTargetsTest {

    @Test
    fun `skipBack subtracts 30 seconds`() {
        assertEquals(70L, SeekTargets.skipBack(100))
    }

    @Test
    fun `skipBack clamps to zero at the start of the book`() {
        assertEquals(0L, SeekTargets.skipBack(0))
        assertEquals(0L, SeekTargets.skipBack(15))
        assertEquals(0L, SeekTargets.skipBack(30))
        assertEquals(1L, SeekTargets.skipBack(31))
    }

    @Test
    fun `skipForward adds 30 seconds`() {
        assertEquals(60L, SeekTargets.skipForward(currentAbsSec = 30, durationSec = 3600))
    }

    @Test
    fun `skipForward clamps to duration at the end of the book`() {
        assertEquals(3600L, SeekTargets.skipForward(currentAbsSec = 3600, durationSec = 3600))
        assertEquals(3600L, SeekTargets.skipForward(currentAbsSec = 3590, durationSec = 3600))
        assertEquals(3599L, SeekTargets.skipForward(currentAbsSec = 3569, durationSec = 3600))
    }
}
