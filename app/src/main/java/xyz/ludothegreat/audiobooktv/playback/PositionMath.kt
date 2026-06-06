package xyz.ludothegreat.audiobooktv.playback

import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsAudioTrack
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter

/**
 * Pure timeline math for the player. Extracted from PlayerViewModel so the
 * absolute-position / per-track seek / chapter-title rules can be tested
 * without a MediaController.
 *
 * ABS exposes audiobooks as a sequence of audio tracks each with a
 * `startOffset` in seconds from the book start. ExoPlayer's position is
 * `(currentMediaItemIndex, currentPositionMs)` — relative to the current
 * track. The "absolute position in the book" is what every user-facing
 * surface (UI, server progress sync, bookmarks) wants. These functions
 * convert between the two.
 */
object PositionMath {

    /**
     * Player position projected onto the book timeline, in whole seconds.
     * Uses the start offset of the currently-playing track plus its local
     * elapsed time.
     */
    fun absolutePositionSec(
        currentPositionMs: Long,
        currentMediaItemIndex: Int,
        tracks: List<AbsAudioTrack>,
    ): Long {
        val trackOffsetSec = tracks.getOrNull(currentMediaItemIndex)?.startOffset ?: 0.0
        return ((currentPositionMs / 1000.0) + trackOffsetSec).toLong()
    }

    /**
     * Inverse of [absolutePositionSec]: given a destination position on the
     * book timeline, return the (trackIndex, withinTrackMs) tuple to feed
     * to MediaController.seekTo. Empty tracks degenerate to (0, absoluteMs)
     * so single-file books still seek correctly.
     */
    fun trackForAbsolute(absoluteMs: Long, tracks: List<AbsAudioTrack>): SeekTarget {
        if (tracks.isEmpty()) return SeekTarget(0, absoluteMs)
        val absoluteSec = absoluteMs / 1000.0
        val track = tracks.indexOfLast { it.startOffset <= absoluteSec }.coerceAtLeast(0)
        val withinTrackSec = absoluteSec - tracks[track].startOffset
        return SeekTarget(
            mediaItemIndex = track,
            positionMs = (withinTrackSec * 1000).toLong().coerceAtLeast(0),
        )
    }

    /**
     * Chapter the player is currently inside, by half-open interval
     * `[start, end)`. ABS chapter boundaries are exclusive on the right,
     * matching how the on-screen title rolls over precisely at the cut.
     */
    fun currentChapterTitle(absSec: Double, chapters: List<AbsChapter>): String {
        val c = chapters.firstOrNull { absSec >= it.start && absSec < it.end }
        return c?.title.orEmpty()
    }
}

data class SeekTarget(val mediaItemIndex: Int, val positionMs: Long)
