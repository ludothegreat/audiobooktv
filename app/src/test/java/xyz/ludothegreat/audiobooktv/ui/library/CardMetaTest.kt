package xyz.ludothegreat.audiobooktv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the pure card-text logic: total-duration formatting, the
 * author-and-duration meta line built from metadata fields only, and the
 * cover-bar fraction clamp.
 */
class CardMetaTest {

    // ---- durationLabel ----

    @Test
    fun `unknown duration gives no label`() {
        assertNull(CardMeta.durationLabel(0))
        assertNull(CardMeta.durationLabel(-5))
    }

    @Test
    fun `sub-minute duration reads 1m not 0m`() {
        assertEquals("1m", CardMeta.durationLabel(29))
        assertEquals("1m", CardMeta.durationLabel(1))
    }

    @Test
    fun `under an hour shows minutes only`() {
        assertEquals("42m", CardMeta.durationLabel(42 * 60L))
    }

    @Test
    fun `seconds round to the nearest minute`() {
        assertEquals("41m", CardMeta.durationLabel(41 * 60L + 29))
        assertEquals("42m", CardMeta.durationLabel(41 * 60L + 30))
    }

    @Test
    fun `rounding carries into the hour instead of printing 60m`() {
        // 10h 59m 40s must read 11h 0m, never 10h 60m.
        assertEquals("11h 0m", CardMeta.durationLabel(10 * 3600L + 59 * 60 + 40))
    }

    @Test
    fun `exact hour shows 0 minutes`() {
        assertEquals("1h 0m", CardMeta.durationLabel(3600))
    }

    @Test
    fun `real library durations format as hours and minutes`() {
        // 42636s (Young Romantics) and 80155s (The Blade Itself) from the
        // live test library.
        assertEquals("11h 51m", CardMeta.durationLabel(42636))
        assertEquals("22h 16m", CardMeta.durationLabel(80155))
    }

    // ---- metaLine ----

    @Test
    fun `author and duration join with a middle dot`() {
        assertEquals("Joe Abercrombie · 22h 16m", CardMeta.metaLine("Joe Abercrombie", 80155))
    }

    @Test
    fun `ABS empty-string author is omitted with no separator`() {
        // The untagged-book shape: authorName is "" and only the duration
        // exists. No guessing, no dangling dot.
        val line = CardMeta.metaLine("", 42636)
        assertEquals("11h 51m", line)
    }

    @Test
    fun `missing duration leaves the author alone`() {
        assertEquals("Katherine Addison", CardMeta.metaLine("Katherine Addison", 0))
    }

    @Test
    fun `nothing known drops the whole line`() {
        assertNull(CardMeta.metaLine(null, 0))
        assertNull(CardMeta.metaLine("  ", -1))
    }

    // ---- barFraction ----

    @Test
    fun `fraction is clamped to the unit interval`() {
        assertEquals(0f, CardMeta.barFraction(-0.25), 0f)
        assertEquals(0.5f, CardMeta.barFraction(0.5), 0f)
        assertEquals(1f, CardMeta.barFraction(1.2), 0f)
    }
}
