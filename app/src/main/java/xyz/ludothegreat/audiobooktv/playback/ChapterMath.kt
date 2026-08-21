package xyz.ludothegreat.audiobooktv.playback

import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import kotlin.math.ceil

/**
 * Pure chapter math for the dual position display and the jump-to-chapter
 * pickers. Mirrors [PositionMath]'s half-open `[start, end)` interval rule
 * so the chapter bar, the chapter title, and the picker highlight can never
 * disagree about which chapter the player is inside.
 *
 * All inputs are the raw ABS chapter records from the play-session payload
 * (seconds as doubles); nothing here touches a MediaController, so every
 * boundary is locked by ChapterMathTest on the JVM.
 */
object ChapterMath {

    /**
     * Index of the chapter containing [absSec], or null when the position is
     * outside every chapter: before the first start, at/past the last end,
     * or when the book has no chapter data at all. Callers treat null as
     * "no chapter UI" -- an empty list must degrade to the plain book bar,
     * never to a zeroed chapter bar.
     */
    fun indexAt(absSec: Double, chapters: List<AbsChapter>): Int? {
        val index = chapters.indexOfFirst { absSec >= it.start && absSec < it.end }
        return if (index >= 0) index else null
    }

    /** Chapter length in seconds. Bad data (end before start) collapses to 0. */
    fun chapterDurationSec(chapter: AbsChapter): Double = (chapter.end - chapter.start).coerceAtLeast(0.0)

    /** Seconds listened inside [chapter], clamped into its envelope. */
    fun elapsedSec(absSec: Double, chapter: AbsChapter): Double = (absSec - chapter.start).coerceIn(0.0, chapterDurationSec(chapter))

    /** Fill fraction for the chapter bar. A zero-length chapter reads as 0. */
    fun progressFraction(absSec: Double, chapter: AbsChapter): Float {
        val duration = chapterDurationSec(chapter)
        if (duration <= 0.0) return 0f
        return (elapsedSec(absSec, chapter) / duration).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Wall-clock seconds until the chapter ends at the given playback rate.
     * Content remaining is divided by the rate because the display promises
     * "how long until the next chapter", not "how much audio is left" --
     * at 2x, 60 content-seconds arrive in 30. Ceil so the label only shows
     * 0:00 at the true end, and a non-positive rate falls back to 1x rather
     * than producing a nonsense negative or infinite countdown.
     */
    fun remainingSecAtSpeed(absSec: Double, chapter: AbsChapter, speed: Float): Long {
        val remainingContent = (chapter.end - absSec).coerceIn(0.0, chapterDurationSec(chapter))
        val rate = if (speed > 0f) speed.toDouble() else 1.0
        return ceil(remainingContent / rate).toLong()
    }

    /** The "-12:34" countdown label all the reference players use. */
    fun remainingLabel(remainingSec: Long): String = "-" + formatTimestampHms(remainingSec)

    /**
     * The "Ch n/N" position counter shown on the player controls of both
     * surfaces. Between chapters (before the first start or in a gap) the
     * index is unknown; the count alone still tells the user how much
     * structure the book has.
     */
    fun counterLabel(chapterIndex: Int?, chapterCount: Int): String =
        if (chapterIndex != null) "Ch ${chapterIndex + 1}/$chapterCount" else "Ch -/$chapterCount"

    /**
     * Whole-second seek target for a chapter jump. Ceil, not truncate: ABS
     * chapter starts are fractional (496.256961), and truncating would land
     * the head 0.26s inside the previous chapter, so the title and bar would
     * show the wrong chapter until the next tick. Ceil lands just inside the
     * chosen chapter at the cost of skipping under a second of audio.
     */
    fun jumpTargetSec(chapter: AbsChapter): Long = ceil(chapter.start).toLong().coerceAtLeast(0)
}
