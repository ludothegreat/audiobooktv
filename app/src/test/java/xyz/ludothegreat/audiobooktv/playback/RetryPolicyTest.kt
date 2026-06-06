package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks decision #16's retry timing: silent for the first 30 seconds of
 * failed retries, then surface the "Reconnecting..." badge. `firstError == 0`
 * is the sentinel for "no error active right now".
 */
class RetryPolicyTest {

    @Test
    fun `no error active hides the badge`() {
        assertFalse(RetryPolicy.shouldShowReconnecting(firstErrorWallClockMs = 0, nowMs = 1_000_000))
    }

    @Test
    fun `error younger than 30s stays silent`() {
        val first = 1_000_000L
        assertFalse(RetryPolicy.shouldShowReconnecting(first, first))
        assertFalse(RetryPolicy.shouldShowReconnecting(first, first + 5_000))
        assertFalse(RetryPolicy.shouldShowReconnecting(first, first + 29_999))
    }

    @Test
    fun `error at exactly 30s shows the badge`() {
        val first = 1_000_000L
        assertTrue(
            RetryPolicy.shouldShowReconnecting(
                firstErrorWallClockMs = first,
                nowMs = first + RetryPolicy.RECONNECTING_BANNER_AFTER_MS,
            ),
        )
    }

    @Test
    fun `error older than 30s shows the badge`() {
        val first = 1_000_000L
        assertTrue(RetryPolicy.shouldShowReconnecting(first, first + 60_000))
    }
}
