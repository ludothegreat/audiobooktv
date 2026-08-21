package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the cross-book bleed guard (resume-integrity case C1): the
 * isPlaying=false that a book switch produces must not flush, and exactly
 * one such callback is suppressed per armed switch.
 *
 * Mutation check: make shouldFlushOnPause() always return true and the
 * suppression tests fail; make it always return false and the plain-pause
 * test fails (which is the case B trap: no sync at pause).
 */
class PauseFlushGateTest {

    @Test
    fun `plain pause flushes`() {
        val gate = PauseFlushGate()
        assertTrue(gate.shouldFlushOnPause())
    }

    @Test
    fun `armed transition suppresses exactly one pause`() {
        val gate = PauseFlushGate()
        gate.armForTransition()
        assertFalse(gate.shouldFlushOnPause())
        assertTrue(gate.shouldFlushOnPause())
    }

    @Test
    fun `suppression does not outlive the switch that armed it`() {
        val gate = PauseFlushGate()
        gate.armForTransition()
        assertFalse(gate.shouldFlushOnPause())
        // A real user pause later the same session must flush: this is the
        // "sync fires late" trap from the reference apps, and it is the
        // failure mode an over-eager suppression would reintroduce.
        assertTrue(gate.shouldFlushOnPause())
        assertTrue(gate.shouldFlushOnPause())
    }

    @Test
    fun `re-arming across consecutive switches suppresses each stop once`() {
        val gate = PauseFlushGate()
        gate.armForTransition()
        assertFalse(gate.shouldFlushOnPause())
        gate.armForTransition()
        assertFalse(gate.shouldFlushOnPause())
        assertTrue(gate.shouldFlushOnPause())
    }

    @Test
    fun `arming is idempotent within one switch`() {
        val gate = PauseFlushGate()
        gate.armForTransition()
        gate.armForTransition()
        assertFalse(gate.shouldFlushOnPause())
        assertTrue(gate.shouldFlushOnPause())
    }
}
