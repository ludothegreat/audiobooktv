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

    @Test
    fun `longSkipBack subtracts 5 minutes`() {
        assertEquals(700L, SeekTargets.longSkipBack(1000))
    }

    @Test
    fun `longSkipBack clamps to zero near the start of the book`() {
        assertEquals(0L, SeekTargets.longSkipBack(0))
        assertEquals(0L, SeekTargets.longSkipBack(299))
        assertEquals(0L, SeekTargets.longSkipBack(300))
        assertEquals(1L, SeekTargets.longSkipBack(301))
    }

    @Test
    fun `longSkipForward adds 5 minutes`() {
        assertEquals(900L, SeekTargets.longSkipForward(currentAbsSec = 600, durationSec = 3600))
    }

    @Test
    fun `longSkipForward clamps to duration near the end of the book`() {
        assertEquals(3600L, SeekTargets.longSkipForward(currentAbsSec = 3600, durationSec = 3600))
        assertEquals(3600L, SeekTargets.longSkipForward(currentAbsSec = 3400, durationSec = 3600))
        assertEquals(3599L, SeekTargets.longSkipForward(currentAbsSec = 3299, durationSec = 3600))
    }

    @Test
    fun `long skip is 10x the primary skip, not a replacement for it`() {
        // The 30s increment is settled as the primary; this pins the pair
        // relationship so a future tweak to one constant is a conscious act.
        assertEquals(30L, SeekTargets.SKIP_SECONDS)
        assertEquals(300L, SeekTargets.LONG_SKIP_SECONDS)
    }
}
