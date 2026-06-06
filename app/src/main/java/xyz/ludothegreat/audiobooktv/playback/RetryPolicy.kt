package xyz.ludothegreat.audiobooktv.playback

/**
 * Net-drop retry timing rules (decision #16):
 * - Pause silently and retry every 5s.
 * - After 30s of failed retries, show a small "Reconnecting..." indicator.
 * - Resume automatically when the server is back.
 *
 * Pure-Kotlin so PlayerViewModel doesn't have to wire its own clock to
 * test the indicator-after-30s rule.
 */
object RetryPolicy {
    const val RETRY_INTERVAL_MS: Long = 5_000L
    const val RECONNECTING_BANNER_AFTER_MS: Long = 30_000L

    /**
     * Was the first error long enough ago that the small "Reconnecting..."
     * indicator should be visible to the user? `firstErrorWallClockMs == 0L`
     * means no error is currently active and the indicator must be off.
     */
    fun shouldShowReconnecting(firstErrorWallClockMs: Long, nowMs: Long): Boolean {
        if (firstErrorWallClockMs == 0L) return false
        return nowMs - firstErrorWallClockMs >= RECONNECTING_BANNER_AFTER_MS
    }
}
