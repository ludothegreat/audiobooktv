package xyz.ludothegreat.audiobooktv.playback

/**
 * Touch-scrubber clamp. Mirrors [SeekTargets] for symmetry: a user-initiated
 * drag-to-position must never escape [0, durationSec]. Pulled out of
 * PlayerViewModel.seekToAbsoluteSec so the clamp boundaries are testable
 * without a MediaController in flight.
 *
 * Negative target inputs are coerced to 0. A duration <= 0 (book hasn't
 * loaded yet) collapses to 0 to refuse the seek -- we don't want a stray
 * scrubber callback during load to land the player at -1s or some
 * Long.MAX_VALUE.
 */
object ScrubTargets {
    fun clamp(targetSec: Long, durationSec: Long): Long {
        if (durationSec <= 0) return 0
        return targetSec.coerceIn(0, durationSec)
    }
}
