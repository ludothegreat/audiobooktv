package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the end-of-chapter sleep semantics: fire once at the first
 * boundary playback crosses on its own, never on a user seek, never
 * when merely entering a chapter from a gap or from unknown.
 */
class SleepEocGateTest {

    @Test
    fun `armed gate fires when playback leaves the tracked chapter`() {
        val gate = SleepEocGate()
        gate.arm(2)
        assertFalse("same chapter must not fire", gate.onChapterTick(2))
        assertTrue("crossing into the next chapter must fire", gate.onChapterTick(3))
    }

    @Test
    fun `crossing from a chapter into a gap fires`() {
        val gate = SleepEocGate()
        gate.arm(2)
        assertTrue("the tracked chapter ended, gap or not", gate.onChapterTick(null))
    }

    @Test
    fun `firing consumes the arm`() {
        val gate = SleepEocGate()
        gate.arm(2)
        assertTrue(gate.onChapterTick(3))
        assertFalse("a consumed gate must stay quiet", gate.onChapterTick(4))
        assertFalse(gate.armed)
    }

    @Test
    fun `unarmed gate never fires`() {
        val gate = SleepEocGate()
        assertFalse(gate.onChapterTick(1))
        assertFalse(gate.onChapterTick(2))
    }

    @Test
    fun `entering a chapter from null tracks without firing`() {
        val gate = SleepEocGate()
        gate.arm(null)
        assertFalse("nothing the user was hearing has ended yet", gate.onChapterTick(3))
        assertTrue("but the end of that newly-entered chapter fires", gate.onChapterTick(4))
    }

    @Test
    fun `reseed after a user seek re-targets without firing`() {
        val gate = SleepEocGate()
        gate.arm(2)
        gate.reseed(5)
        assertFalse("the seek landing is the new tracked chapter", gate.onChapterTick(5))
        assertTrue(gate.onChapterTick(6))
    }

    @Test
    fun `reseed while disarmed stays disarmed`() {
        val gate = SleepEocGate()
        gate.reseed(3)
        assertFalse(gate.armed)
        assertFalse(gate.onChapterTick(4))
    }

    @Test
    fun `disarm is a hard off switch`() {
        val gate = SleepEocGate()
        gate.arm(2)
        gate.disarm()
        assertFalse(gate.onChapterTick(3))
        assertFalse(gate.armed)
    }
}
