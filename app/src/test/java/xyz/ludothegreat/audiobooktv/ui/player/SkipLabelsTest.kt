package xyz.ludothegreat.audiobooktv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.ludothegreat.audiobooktv.playback.SeekTargets

/**
 * Locks the uniform value-plus-unit skip label pattern on the increments
 * the player actually uses. Literal expected strings on purpose: asserting
 * back == guillemet + amount would be true by construction.
 */
class SkipLabelsTest {

    @Test
    fun `the locked 30s increment labels with its unit`() {
        assertEquals("30s", SkipLabels.amount(SeekTargets.SKIP_SECONDS))
        assertEquals("«30s", SkipLabels.back(SeekTargets.SKIP_SECONDS))
        assertEquals("30s»", SkipLabels.forward(SeekTargets.SKIP_SECONDS))
    }

    @Test
    fun `the 5m long skip labels in minutes`() {
        assertEquals("5m", SkipLabels.amount(SeekTargets.LONG_SKIP_SECONDS))
        assertEquals("«5m", SkipLabels.back(SeekTargets.LONG_SKIP_SECONDS))
        assertEquals("5m»", SkipLabels.forward(SeekTargets.LONG_SKIP_SECONDS))
    }

    @Test
    fun `whole minutes read as minutes`() {
        assertEquals("1m", SkipLabels.amount(60))
        assertEquals("10m", SkipLabels.amount(600))
    }

    @Test
    fun `partial minutes stay honest in seconds`() {
        assertEquals("90s", SkipLabels.amount(90))
        assertEquals("45s", SkipLabels.amount(45))
    }
}
