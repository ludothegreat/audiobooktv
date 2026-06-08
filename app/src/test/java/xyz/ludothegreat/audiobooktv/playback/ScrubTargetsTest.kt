package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrubTargetsTest {

    @Test
    fun `in-range target is returned unchanged`() {
        assertEquals(120L, ScrubTargets.clamp(targetSec = 120, durationSec = 3600))
    }

    @Test
    fun `negative target clamps to zero`() {
        assertEquals(0L, ScrubTargets.clamp(targetSec = -10, durationSec = 3600))
    }

    @Test
    fun `target past duration clamps to duration`() {
        assertEquals(3600L, ScrubTargets.clamp(targetSec = 9_000, durationSec = 3600))
    }

    @Test
    fun `zero or negative duration refuses the seek`() {
        // Stray scrubber callback during load must not land at a wild value.
        assertEquals(0L, ScrubTargets.clamp(targetSec = 100, durationSec = 0))
        assertEquals(0L, ScrubTargets.clamp(targetSec = 100, durationSec = -1))
    }
}
