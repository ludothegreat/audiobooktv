package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the crash-during-network-drop recovery contract (resume-integrity
 * case E): a DIRTY local record strictly ahead of the server is the ONLY
 * shape that outranks the server value, and it does so by pushing, never by
 * silently diverging. Everything else defers to the server, because position
 * is server-truth.
 *
 * Mutation check for this suite: make reconcile() always return
 * UseServer(serverSec) and the push tests fail; flip `<=` to `<` on the
 * server comparison and the equal-position test fails.
 */
class PositionReconcilerTest {

    private fun record(
        itemId: String = "book-1",
        positionSec: Double = 500.0,
        dirty: Boolean = true,
    ) = LocalPositionRecord(itemId = itemId, positionSec = positionSec, recordedAtMs = 1_000L, dirty = dirty)

    private fun reconcile(
        serverSec: Double,
        local: LocalPositionRecord?,
        itemId: String = "book-1",
        durationSec: Double = 3_600.0,
    ) = PositionReconciler.reconcile(itemId = itemId, serverSec = serverSec, durationSec = durationSec, local = local)

    @Test
    fun `no local record defers to the server`() {
        assertEquals(ResumeDecision.UseServer(400.0), reconcile(serverSec = 400.0, local = null))
    }

    @Test
    fun `record for a different book defers to the server`() {
        assertEquals(
            ResumeDecision.UseServer(400.0),
            reconcile(serverSec = 400.0, local = record(itemId = "book-2", positionSec = 900.0)),
        )
    }

    @Test
    fun `clean record ahead of the server still defers to the server`() {
        // Clean means the last sync landed, so a lower server value is another
        // client's deliberate rewind. Following it is correct; pushing our old
        // position over it would fight the user.
        assertEquals(
            ResumeDecision.UseServer(400.0),
            reconcile(serverSec = 400.0, local = record(positionSec = 900.0, dirty = false)),
        )
    }

    @Test
    fun `dirty record behind the server defers to the server`() {
        // Another device listened further while we were offline. Furthest wins.
        assertEquals(
            ResumeDecision.UseServer(1_200.0),
            reconcile(serverSec = 1_200.0, local = record(positionSec = 900.0)),
        )
    }

    @Test
    fun `dirty record equal to the server defers to the server`() {
        assertEquals(
            ResumeDecision.UseServer(900.0),
            reconcile(serverSec = 900.0, local = record(positionSec = 900.0)),
        )
    }

    @Test
    fun `dirty record ahead of the server is adopted and pushed`() {
        // The one shape that outranks the server: we listened past the last
        // confirmed sync and the process died. Silently seeking back to 400
        // is the data loss this reconciler exists to prevent.
        assertEquals(
            ResumeDecision.UseLocalAndPush(900.0),
            reconcile(serverSec = 400.0, local = record(positionSec = 900.0)),
        )
    }

    @Test
    fun `adopting is distinguishable from deferring at the same position`() {
        // The decision TYPE carries the push obligation. A refactor that
        // collapses both cases into a bare position would drop the push and
        // quietly re-open the stale-server window.
        val adopted = reconcile(serverSec = 400.0, local = record(positionSec = 900.0))
        val deferred = reconcile(serverSec = 900.0, local = null)
        assertEquals(adopted.positionSec, deferred.positionSec, 0.0)
        assertEquals(true, adopted is ResumeDecision.UseLocalAndPush)
        assertEquals(true, deferred is ResumeDecision.UseServer)
    }

    @Test
    fun `dirty record past the known duration is distrusted entirely`() {
        // ScrubTargets lesson: a value that cannot be right is refused, not
        // clamped into a plausible-looking one.
        assertEquals(
            ResumeDecision.UseServer(400.0),
            reconcile(serverSec = 400.0, local = record(positionSec = 4_000.0), durationSec = 3_600.0),
        )
    }

    @Test
    fun `negative dirty record is distrusted entirely`() {
        assertEquals(
            ResumeDecision.UseServer(-10.0),
            reconcile(serverSec = -10.0, local = record(positionSec = -2.0)),
        )
    }

    @Test
    fun `unknown duration refuses the local record`() {
        // Without a duration the plausibility check cannot run, so the local
        // record is not trusted. Same contract as ScrubTargets.clamp during
        // load: no duration, no position of our own making.
        assertEquals(
            ResumeDecision.UseServer(400.0),
            reconcile(serverSec = 400.0, local = record(positionSec = 900.0), durationSec = 0.0),
        )
    }
}
