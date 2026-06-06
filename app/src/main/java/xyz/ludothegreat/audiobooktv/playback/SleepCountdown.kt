package xyz.ludothegreat.audiobooktv.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sleep-timer countdown. Independent of any player; takes a scope and
 * two callbacks: one for each tick (remaining ms, or null when no timer
 * is set) and one when the timer reaches zero.
 *
 * Lifecycle rules:
 *
 *   setDuration(minutes)  resets remaining to (minutes * 60_000) and
 *                         cancels any active job. minutes <= 0 turns
 *                         the timer off entirely (remaining = null).
 *   resume()              kicks off the per-second tick loop. No-op if
 *                         no duration is set.
 *   pause()               cancels the tick loop but preserves remaining
 *                         so the next resume() picks up where it stopped.
 *
 * The countdown ticks once per second. When remaining hits zero, the
 * countdown calls onFire exactly once and resets remaining to null
 * (so a subsequent resume() does nothing until setDuration is called
 * again).
 *
 * The active-countdown state is intentionally NOT persisted across app
 * close (decision: the preset persists, the live tick does not). On
 * cold start, the PlayerViewModel reads the preset and calls
 * setDuration; resume() fires only after the player actually plays.
 */
class SleepCountdown(
    private val scope: CoroutineScope,
    private val onFire: () -> Unit,
    private val onRemainingChanged: (remainingMs: Long?) -> Unit,
    private val tickIntervalMs: Long = 1_000L,
) {
    private var remainingMs: Long? = null
    private var job: Job? = null

    val active: Boolean get() = (remainingMs ?: 0) > 0
    val remaining: Long? get() = remainingMs

    fun setDuration(minutes: Int) {
        job?.cancel()
        job = null
        remainingMs = if (minutes > 0) minutes * 60_000L else null
        onRemainingChanged(remainingMs)
    }

    fun resume() {
        val current = remainingMs ?: return
        if (current <= 0) return
        job?.cancel()
        job = scope.launch {
            while ((remainingMs ?: 0) > 0) {
                delay(tickIntervalMs)
                val newMs = ((remainingMs ?: 0) - tickIntervalMs).coerceAtLeast(0)
                remainingMs = newMs
                onRemainingChanged(newMs.takeIf { it > 0 })
                if (newMs == 0L) {
                    remainingMs = null
                    onFire()
                    return@launch
                }
            }
        }
    }

    fun pause() {
        job?.cancel()
        job = null
    }
}
