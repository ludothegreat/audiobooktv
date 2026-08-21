package xyz.ludothegreat.audiobooktv.playback

/**
 * A locally persisted playhead position, written every time the app attempts
 * a progress sync. `dirty` means the last sync attempt for this record was
 * never confirmed by the server: the write happens before the network call
 * and the record is only marked clean after a 2xx. A dirty record is
 * therefore progress the server may never have seen.
 */
data class LocalPositionRecord(
    val itemId: String,
    val positionSec: Double,
    val recordedAtMs: Long,
    val dirty: Boolean,
)

/**
 * What to do with a server-reported position, given what we know locally.
 * `positionSec` is always the position to adopt.
 */
sealed interface ResumeDecision {
    val positionSec: Double

    /** The server value stands. This is the default: position is server-truth. */
    data class UseServer(override val positionSec: Double) : ResumeDecision

    /**
     * The local record outran the server while syncs were failing. Adopt the
     * local value AND push it, so the server converges forward to the
     * furthest-played point instead of yanking this device backward.
     */
    data class UseLocalAndPush(override val positionSec: Double) : ResumeDecision
}

/**
 * Decides between a server-reported position and the locally cached one.
 *
 * Position is server-truth, so the server wins in every case
 * except one: a DIRTY local record strictly ahead of the server. That shape
 * only arises when this device listened past its last confirmed sync (network
 * drop, then crash or kill), and following the server would silently discard
 * listened progress. The resolution still honors server-truth: the local
 * value is adopted AND pushed, making the server converge forward.
 *
 * A dirty record that is implausible (negative, past the known duration) is
 * distrusted entirely rather than clamped into plausibility. Same lesson as
 * ScrubTargets: never fabricate a plausible-looking position out of a value
 * that cannot be right.
 */
object PositionReconciler {
    fun reconcile(
        itemId: String,
        serverSec: Double,
        durationSec: Double,
        local: LocalPositionRecord?,
    ): ResumeDecision {
        val candidate = local?.takeIf { it.itemId == itemId && it.dirty }
            ?: return ResumeDecision.UseServer(serverSec)
        val plausible = candidate.positionSec >= 0 &&
            durationSec > 0 &&
            candidate.positionSec <= durationSec
        if (plausible && candidate.positionSec > serverSec) {
            return ResumeDecision.UseLocalAndPush(candidate.positionSec)
        }
        return ResumeDecision.UseServer(serverSec)
    }
}
