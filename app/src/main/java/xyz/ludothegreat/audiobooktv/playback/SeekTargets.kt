package xyz.ludothegreat.audiobooktv.playback

/**
 * Skip-button policy (decision #11): skip-back-30s, skip-forward-30s,
 * clamped to the book's [0, durationSec] envelope. Pulled out of
 * PlayerViewModel so the clamp boundaries are testable and a future
 * refactor that loosens "30" to a setting won't silently allow seeks
 * past the end of the book.
 */
object SeekTargets {
    const val SKIP_SECONDS: Long = 30L

    fun skipBack(currentAbsSec: Long): Long = (currentAbsSec - SKIP_SECONDS).coerceAtLeast(0)

    fun skipForward(currentAbsSec: Long, durationSec: Long): Long = (currentAbsSec + SKIP_SECONDS).coerceAtMost(durationSec)
}
