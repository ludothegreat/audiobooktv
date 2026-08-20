package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        // null, NOT 0. seekToAbsoluteSec pushes every clamped target to the
        // server, so returning 0 here would sync position 0 to Audiobookshelf
        // and wipe the listener's progress on a stray callback during load.
        assertNull(ScrubTargets.clamp(targetSec = 100, durationSec = 0))
        assertNull(ScrubTargets.clamp(targetSec = 100, durationSec = -1))
    }

    @Test
    fun `refusal is distinguishable from a real seek to start`() {
        assertEquals(0L, ScrubTargets.clamp(targetSec = 0, durationSec = 3600))
        assertNull(ScrubTargets.clamp(targetSec = 0, durationSec = 0))
    }
}
