package xyz.ludothegreat.audiobooktv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A screen reader gets one sentence per tile instead of the badge, title and
 * caption fragments a sighted user assembles visually. Locking the wording
 * down here keeps the spoken form from drifting when the visual card changes.
 */
class CardMetaSpokenTest {

    @Test
    fun `started book announces title author length and progress`() {
        assertEquals(
            "2. You Have to Eat, by Adam Mansbach, 8h 53m, 70% complete",
            CardMeta.spokenSummary(
                title = "2. You Have to Eat",
                author = "Adam Mansbach",
                durationSec = 32_000,
                percent = "70%",
                finished = false,
            ),
        )
    }

    @Test
    fun `finished book says finished rather than a percentage`() {
        assertEquals(
            "The God Test, 11h 5m, finished",
            CardMeta.spokenSummary(
                title = "The God Test",
                author = null,
                durationSec = 39_921,
                percent = "99%",
                finished = true,
            ),
        )
    }

    @Test
    fun `unstarted book says not started`() {
        assertEquals(
            "Daemon, by Daniel Suarez, 15h 56m, not started",
            CardMeta.spokenSummary(
                title = "Daemon",
                author = "Daniel Suarez",
                durationSec = 57_360,
                percent = null,
                finished = false,
            ),
        )
    }

    @Test
    fun `a missing author is omitted rather than spoken as blank`() {
        val spoken = CardMeta.spokenSummary(
            title = "Untagged",
            author = "",
            durationSec = 3_600,
            percent = null,
            finished = false,
        )
        assertEquals("Untagged, 1h 0m, not started", spoken)
    }
}
