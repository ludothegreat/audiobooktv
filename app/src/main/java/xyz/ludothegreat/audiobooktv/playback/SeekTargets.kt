package xyz.ludothegreat.audiobooktv.playback

/**
 * Skip-button policy (decision #11 plus the dual-granularity add):
 * a short 30s pair for sentence-level correction and a long 5m pair for
 * getting across a chapter, both clamped to the book's [0, durationSec]
 * envelope. Pulled out of PlayerViewModel so the clamp boundaries are
 * testable and a future refactor that loosens the amounts to settings
 * won't silently allow seeks past the end of the book.
 *
 * The 30s increment is the locked primary (settled decision); LONG_SKIP
 * adds a second labeled granularity beside it, it does not replace it.
 */
object SeekTargets {
    const val SKIP_SECONDS: Long = 30L
    const val LONG_SKIP_SECONDS: Long = 300L

    fun skipBack(currentAbsSec: Long): Long = back(currentAbsSec, SKIP_SECONDS)

    fun skipForward(currentAbsSec: Long, durationSec: Long): Long = forward(currentAbsSec, durationSec, SKIP_SECONDS)

    fun longSkipBack(currentAbsSec: Long): Long = back(currentAbsSec, LONG_SKIP_SECONDS)

    fun longSkipForward(currentAbsSec: Long, durationSec: Long): Long = forward(currentAbsSec, durationSec, LONG_SKIP_SECONDS)

    private fun back(currentAbsSec: Long, amountSec: Long): Long = (currentAbsSec - amountSec).coerceAtLeast(0)

    private fun forward(currentAbsSec: Long, durationSec: Long, amountSec: Long): Long = (currentAbsSec + amountSec).coerceAtMost(durationSec)
}
