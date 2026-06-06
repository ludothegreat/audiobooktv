package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsAudioTrack
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter

/**
 * Locks the ABS-track <-> ExoPlayer-position math. These rules are how the
 * UI position bar, progress sync, and bookmark seeks all stay consistent
 * with the rest of the audiobookshelf ecosystem.
 */
class PositionMathTest {

    private fun track(idx: Int, startOffsetSec: Double) = AbsAudioTrack(
        index = idx,
        ino = null,
        title = "track-$idx",
        mimeType = "audio/mp4",
        codec = null,
        duration = 100.0,
        startOffset = startOffsetSec,
        contentUrl = "/api/items/x/file/$idx",
    )

    @Test
    fun `single-track book treats player position as absolute`() {
        val tracks = listOf(track(0, 0.0))
        val abs = PositionMath.absolutePositionSec(
            currentPositionMs = 12_500,
            currentMediaItemIndex = 0,
            tracks = tracks,
        )
        assertEquals(12L, abs)
    }

    @Test
    fun `multi-track absolute position adds startOffset to track position`() {
        val tracks = listOf(track(0, 0.0), track(1, 100.0), track(2, 200.0))
        val abs = PositionMath.absolutePositionSec(
            currentPositionMs = 30_000,
            currentMediaItemIndex = 2,
            tracks = tracks,
        )
        assertEquals(230L, abs)
    }

    @Test
    fun `absolute position with out-of-bounds index falls back to zero offset`() {
        val tracks = listOf(track(0, 0.0))
        val abs = PositionMath.absolutePositionSec(
            currentPositionMs = 1_000,
            currentMediaItemIndex = 5,
            tracks = tracks,
        )
        assertEquals(1L, abs)
    }

    @Test
    fun `trackForAbsolute picks the track whose offset is the largest under target`() {
        val tracks = listOf(track(0, 0.0), track(1, 100.0), track(2, 200.0))
        val target = PositionMath.trackForAbsolute(155_000L, tracks)
        assertEquals(1, target.mediaItemIndex)
        assertEquals(55_000L, target.positionMs)
    }

    @Test
    fun `trackForAbsolute on exact boundary picks the new track`() {
        val tracks = listOf(track(0, 0.0), track(1, 100.0))
        val target = PositionMath.trackForAbsolute(100_000L, tracks)
        assertEquals(1, target.mediaItemIndex)
        assertEquals(0L, target.positionMs)
    }

    @Test
    fun `trackForAbsolute with empty tracks degenerates to single seek`() {
        val target = PositionMath.trackForAbsolute(45_000L, emptyList())
        assertEquals(0, target.mediaItemIndex)
        assertEquals(45_000L, target.positionMs)
    }

    @Test
    fun `trackForAbsolute clamps negative seeks to zero within track`() {
        val tracks = listOf(track(0, 100.0), track(1, 200.0))
        // Target before any track's startOffset (50s, but first track starts at 100s)
        // The indexOfLast picks 0 with coerceAtLeast(0); withinTrack would be -50s.
        val target = PositionMath.trackForAbsolute(50_000L, tracks)
        assertEquals(0, target.mediaItemIndex)
        assertEquals(0L, target.positionMs)
    }

    @Test
    fun `currentChapterTitle returns chapter whose half-open interval contains time`() {
        val chapters = listOf(
            AbsChapter(id = 1, start = 0.0, end = 100.0, title = "Chapter One"),
            AbsChapter(id = 2, start = 100.0, end = 200.0, title = "Chapter Two"),
            AbsChapter(id = 3, start = 200.0, end = 300.0, title = "Chapter Three"),
        )
        assertEquals("Chapter One", PositionMath.currentChapterTitle(50.0, chapters))
        // Boundary is right-exclusive: 100.0 falls inside Chapter Two.
        assertEquals("Chapter Two", PositionMath.currentChapterTitle(100.0, chapters))
        assertEquals("Chapter Three", PositionMath.currentChapterTitle(299.9, chapters))
    }

    @Test
    fun `currentChapterTitle returns empty when no chapters match`() {
        val chapters = listOf(AbsChapter(id = 1, start = 0.0, end = 100.0, title = "Only"))
        assertEquals("", PositionMath.currentChapterTitle(100.0, chapters))
        assertEquals("", PositionMath.currentChapterTitle(-5.0, chapters))
        assertEquals("", PositionMath.currentChapterTitle(0.0, emptyList()))
    }

    @Test
    fun `currentChapterTitle handles null title gracefully`() {
        val chapters = listOf(AbsChapter(id = 1, start = 0.0, end = 100.0, title = null))
        assertEquals("", PositionMath.currentChapterTitle(50.0, chapters))
    }
}
