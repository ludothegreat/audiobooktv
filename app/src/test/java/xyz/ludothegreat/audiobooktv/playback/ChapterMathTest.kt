package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter

/**
 * Locks the chapter-boundary rules for the dual position display and the
 * jump-to-chapter pickers. The intervals are half-open `[start, end)` to
 * match PositionMath.currentChapterTitle -- if these ever diverge, the bar
 * and the title would name different chapters at a boundary.
 */
class ChapterMathTest {

    private fun chapter(id: Int, start: Double, end: Double) = AbsChapter(id = id, start = start, end = end, title = "Chapter $id")

    private val three = listOf(
        chapter(0, 0.0, 100.0),
        chapter(1, 100.0, 200.0),
        chapter(2, 200.0, 300.0),
    )

    @Test
    fun `position inside a chapter resolves to its index`() {
        assertEquals(0, ChapterMath.indexAt(50.0, three))
        assertEquals(2, ChapterMath.indexAt(250.0, three))
    }

    @Test
    fun `position exactly at a boundary belongs to the starting chapter`() {
        // Right-exclusive: 100.0 is the first instant of chapter 1, not the
        // last instant of chapter 0.
        assertEquals(1, ChapterMath.indexAt(100.0, three))
        assertEquals(0, ChapterMath.indexAt(0.0, three))
    }

    @Test
    fun `position before the first chapter start resolves to no chapter`() {
        val late = listOf(chapter(0, 10.0, 100.0))
        assertNull(ChapterMath.indexAt(5.0, late))
        assertNull(ChapterMath.indexAt(-1.0, three))
    }

    @Test
    fun `position at or past the last chapter end resolves to no chapter`() {
        assertNull(ChapterMath.indexAt(300.0, three))
        assertNull(ChapterMath.indexAt(9_999.0, three))
    }

    @Test
    fun `empty chapter list resolves to no chapter`() {
        assertNull(ChapterMath.indexAt(0.0, emptyList()))
    }

    @Test
    fun `single chapter book resolves inside and refuses outside`() {
        val single = listOf(chapter(0, 0.0, 300.0))
        assertEquals(0, ChapterMath.indexAt(0.0, single))
        assertEquals(0, ChapterMath.indexAt(299.9, single))
        assertNull(ChapterMath.indexAt(300.0, single))
    }

    @Test
    fun `elapsed clamps into the chapter envelope`() {
        val ch = chapter(1, 100.0, 200.0)
        assertEquals(40.0, ChapterMath.elapsedSec(140.0, ch), 1e-9)
        assertEquals(0.0, ChapterMath.elapsedSec(50.0, ch), 1e-9)
        assertEquals(100.0, ChapterMath.elapsedSec(500.0, ch), 1e-9)
    }

    @Test
    fun `progress fraction spans zero to one and survives zero-length chapters`() {
        val ch = chapter(1, 100.0, 200.0)
        assertEquals(0.4f, ChapterMath.progressFraction(140.0, ch), 1e-6f)
        assertEquals(0f, ChapterMath.progressFraction(100.0, ch), 0f)
        assertEquals(1f, ChapterMath.progressFraction(200.0, ch), 0f)
        assertEquals(0f, ChapterMath.progressFraction(100.0, chapter(2, 100.0, 100.0)), 0f)
    }

    @Test
    fun `remaining divides content seconds by the playback rate`() {
        val ch = chapter(0, 0.0, 100.0)
        assertEquals(60L, ChapterMath.remainingSecAtSpeed(40.0, ch, 1.0f))
        assertEquals(30L, ChapterMath.remainingSecAtSpeed(40.0, ch, 2.0f))
        assertEquals(48L, ChapterMath.remainingSecAtSpeed(40.0, ch, 1.25f))
    }

    @Test
    fun `remaining rounds up so zero only shows at the true end`() {
        val ch = chapter(0, 0.0, 100.0)
        // 100 content-seconds at 1.5x is 66.67 wall seconds -> 67.
        assertEquals(67L, ChapterMath.remainingSecAtSpeed(0.0, ch, 1.5f))
        assertEquals(0L, ChapterMath.remainingSecAtSpeed(100.0, ch, 1.5f))
    }

    @Test
    fun `remaining treats a non-positive rate as 1x`() {
        val ch = chapter(0, 0.0, 100.0)
        assertEquals(60L, ChapterMath.remainingSecAtSpeed(40.0, ch, 0f))
        assertEquals(60L, ChapterMath.remainingSecAtSpeed(40.0, ch, -1f))
    }

    @Test
    fun `remaining label carries the leading minus`() {
        assertEquals("-12:34", ChapterMath.remainingLabel(754))
        assertEquals("-1:02:03", ChapterMath.remainingLabel(3_723))
        assertEquals("-0:00", ChapterMath.remainingLabel(0))
    }

    @Test
    fun `jump target on a fractional start lands inside the chosen chapter`() {
        // Real ABS boundary from The Blade Itself: chapter 1 starts at
        // 496.256961. Truncation would give 496, which indexAt resolves to
        // chapter 0 -- the wrong chapter until the next tick.
        val chapters = listOf(
            chapter(0, 0.0, 496.256961),
            chapter(1, 496.256961, 508.256961),
        )
        val target = ChapterMath.jumpTargetSec(chapters[1])
        assertEquals(497L, target)
        assertEquals(1, ChapterMath.indexAt(target.toDouble(), chapters))
    }

    @Test
    fun `jump target on a whole-second start is that second`() {
        assertEquals(100L, ChapterMath.jumpTargetSec(chapter(1, 100.0, 200.0)))
        assertEquals(0L, ChapterMath.jumpTargetSec(chapter(0, 0.0, 100.0)))
    }

    @Test
    fun `jump target refuses to go negative on bad data`() {
        assertTrue(ChapterMath.jumpTargetSec(chapter(0, -5.0, 100.0)) >= 0L)
    }

    @Test
    fun `chapter duration collapses inverted bad data to zero`() {
        assertEquals(0.0, ChapterMath.chapterDurationSec(chapter(0, 200.0, 100.0)), 1e-9)
        assertEquals(100.0, ChapterMath.chapterDurationSec(chapter(0, 100.0, 200.0)), 1e-9)
    }
}
