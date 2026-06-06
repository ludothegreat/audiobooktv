package xyz.ludothegreat.audiobooktv.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the sleep-timer countdown lifecycle:
 *
 *   setDuration  resets remaining to (minutes * 60s), 0 cancels.
 *   resume       starts ticking; pause halts without resetting.
 *   firing       calls onFire exactly once at zero, clears remaining.
 *   resetting    on new playback restores the selected preset duration.
 *
 * runs on a StandardTestDispatcher so the delay() loop is driven by
 * virtual time, not the wall clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SleepCountdownTest {

    @Test
    fun `setDuration with positive minutes seeds remaining and tick callback`() = runTest {
        val ticks = mutableListOf<Long?>()
        val timer = SleepCountdown(this, onFire = {}, onRemainingChanged = { ticks += it })
        timer.setDuration(5)
        assertEquals(listOf(5 * 60_000L), ticks)
    }

    @Test
    fun `setDuration zero clears remaining and tick fires null`() = runTest {
        val ticks = mutableListOf<Long?>()
        val timer = SleepCountdown(this, onFire = {}, onRemainingChanged = { ticks += it })
        timer.setDuration(5)
        ticks.clear()
        timer.setDuration(0)
        assertEquals(listOf<Long?>(null), ticks)
    }

    @Test
    fun `resume ticks down by 1 second per tick and fires at zero`() = runTest {
        val ticks = mutableListOf<Long?>()
        var fired = 0
        val timer = SleepCountdown(this, onFire = { fired++ }, onRemainingChanged = { ticks += it })
        timer.setDuration(1) // 60_000 ms
        timer.resume()

        advanceTimeBy(60_500)
        advanceUntilIdle()

        assertEquals("countdown should fire exactly once", 1, fired)
        // Last tick reported must be null (cleared) so the UI hides the badge.
        assertNull(ticks.last())
        assertFalse("countdown must clear active when it fires", timer.active)
    }

    @Test
    fun `pause halts ticking but preserves remaining`() = runTest {
        var fired = 0
        var lastTick: Long? = null
        val timer = SleepCountdown(this, onFire = { fired++ }, onRemainingChanged = { lastTick = it })
        timer.setDuration(2) // 120_000 ms

        timer.resume()
        advanceTimeBy(30_000)
        timer.pause()
        val pausedAt = lastTick

        // Time keeps flowing in the dispatcher but no ticks should land.
        advanceTimeBy(60_000)
        assertEquals(pausedAt, lastTick)
        assertEquals(0, fired)

        // Resume should keep counting from the paused remaining.
        timer.resume()
        advanceTimeBy(90_500)
        advanceUntilIdle()

        assertEquals(1, fired)
    }

    @Test
    fun `setDuration to a new preset resets the countdown`() = runTest {
        var fired = 0
        val ticks = mutableListOf<Long?>()
        val timer = SleepCountdown(this, onFire = { fired++ }, onRemainingChanged = { ticks += it })

        timer.setDuration(1)
        timer.resume()
        advanceTimeBy(30_000)

        // User picks a different preset mid-countdown.
        timer.setDuration(5)
        // setDuration cancels the active job; new resume should run the full 5min.
        timer.resume()
        advanceTimeBy(60_000)
        assertEquals(0, fired)
        advanceTimeBy(4 * 60_000L + 1000)
        advanceUntilIdle()

        assertEquals(1, fired)
    }

    @Test
    fun `setDuration zero while active cancels without firing onFire`() = runTest {
        var fired = 0
        val timer = SleepCountdown(this, onFire = { fired++ }, onRemainingChanged = {})
        timer.setDuration(5)
        timer.resume()
        advanceTimeBy(30_000)

        timer.setDuration(0)
        advanceTimeBy(10 * 60_000L)
        advanceUntilIdle()

        assertEquals("Off must not trigger onFire", 0, fired)
        assertFalse(timer.active)
        assertNull(timer.remaining)
    }

    @Test
    fun `resume is no-op when no duration is set`() = runTest {
        var fired = 0
        val ticks = mutableListOf<Long?>()
        val timer = SleepCountdown(this, onFire = { fired++ }, onRemainingChanged = { ticks += it })
        timer.resume()
        advanceTimeBy(60_000)
        assertEquals(0, fired)
        assertTrue("no tick should land for an unset timer", ticks.isEmpty())
    }
}
