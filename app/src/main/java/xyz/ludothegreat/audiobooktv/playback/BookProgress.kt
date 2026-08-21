package xyz.ludothegreat.audiobooktv.playback

import kotlin.math.ceil

/**
 * Book-level progress math for the player's position display: the percent
 * label and the speed-aware "-9:52:10" countdown that replaced the static
 * total duration on the bar's right endpoint. Mirrors [ChapterMath]'s
 * remaining-at-speed semantics so the book bar and the chapter bar can
 * never disagree about how rate affects a countdown.
 */
object BookProgress {

    /**
     * Whole percent listened, floored and clamped to 0..100. Floored so the
     * label only says 100% at the true end; an unknown or bogus duration
     * (<= 0) reads as 0 rather than dividing by zero.
     */
    fun percent(positionSec: Long, durationSec: Long): Int {
        if (durationSec <= 0) return 0
        val fraction = positionSec.toDouble() / durationSec.toDouble()
        return (fraction * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Wall-clock seconds until the book ends at the given playback rate.
     * Content remaining divided by rate, because the display promises "how
     * long until it's over", not "how much audio is left": at 2x, 60
     * content-seconds arrive in 30. Ceil so 0:00 only shows at the true
     * end; a non-positive rate falls back to 1x instead of producing a
     * nonsense countdown; unknown duration reads as 0 remaining.
     */
    fun remainingSecAtSpeed(positionSec: Long, durationSec: Long, speed: Float): Long {
        if (durationSec <= 0) return 0
        val remainingContent = (durationSec - positionSec).coerceIn(0, durationSec).toDouble()
        val rate = if (speed > 0f) speed.toDouble() else 1.0
        return ceil(remainingContent / rate).toLong()
    }
}
