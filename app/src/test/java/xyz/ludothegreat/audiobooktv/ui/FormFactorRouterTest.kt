package xyz.ludothegreat.audiobooktv.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the rule that decides which presentation surface mounts in
 * MainActivity. A regression here will silently land the touch UI on TVs or
 * the TV UI on phones -- both observable but neither immediately broken,
 * so a test pins the contract instead of relying on manual eyeballing.
 */
class FormFactorRouterTest {

    @Test
    fun `leanback present routes to TV`() {
        assertEquals(UiSurface.Tv, FormFactorRouter.choose(hasLeanback = true))
    }

    @Test
    fun `no leanback routes to touch`() {
        assertEquals(UiSurface.Touch, FormFactorRouter.choose(hasLeanback = false))
    }
}
