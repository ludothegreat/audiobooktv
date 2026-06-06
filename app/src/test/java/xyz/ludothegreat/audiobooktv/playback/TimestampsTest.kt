package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Player UI + ABS bookmark labels share this formatter; if the rule
 * drifts (zero-padding, hour-drop threshold) we lose the visible match
 * between the on-screen timestamp and the persisted bookmark label.
 */
class TimestampsTest {

    @Test
    fun `under one hour uses MM colon SS`() {
        assertEquals("0:00", formatTimestampHms(0))
        assertEquals("0:05", formatTimestampHms(5))
        assertEquals("0:59", formatTimestampHms(59))
        assertEquals("1:00", formatTimestampHms(60))
        assertEquals("12:34", formatTimestampHms(12 * 60 + 34))
        assertEquals("59:59", formatTimestampHms(59 * 60 + 59))
    }

    @Test
    fun `at one hour or more uses H colon MM colon SS`() {
        assertEquals("1:00:00", formatTimestampHms(3600))
        assertEquals("1:00:01", formatTimestampHms(3601))
        assertEquals("2:30:45", formatTimestampHms(2 * 3600 + 30 * 60 + 45))
        assertEquals("12:00:00", formatTimestampHms(12 * 3600))
    }

    @Test
    fun `negative seconds render as zero`() {
        assertEquals("0:00", formatTimestampHms(-1))
        assertEquals("0:00", formatTimestampHms(-3600))
    }
}
