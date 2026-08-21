package xyz.ludothegreat.audiobooktv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the pure card-text logic: total-duration formatting, the
 * two-slot author-and-duration meta row built from metadata fields only,
 * and the cover-bar fraction clamp.
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

    // ---- metaParts ----

    @Test
    fun `author and duration land in separate slots`() {
        // Separate slots are the whole fix for duration truncation: the UI
        // gives the duration a rigid slot only because the two values are
        // never joined into one ellipsizable string.
        val parts = CardMeta.metaParts("Joe Abercrombie", 80155)
        assertEquals(CardMeta.MetaParts(author = "Joe Abercrombie", duration = "22h 16m"), parts)
    }

    @Test
    fun `ABS empty-string author leaves a duration-only row`() {
        // The untagged-book shape: authorName is "" and only the duration
        // exists. No guessing, no fabricated author.
        val parts = CardMeta.metaParts("", 42636)
        assertEquals(CardMeta.MetaParts(author = null, duration = "11h 51m"), parts)
    }

    @Test
    fun `missing duration leaves the author alone`() {
        val parts = CardMeta.metaParts("Katherine Addison", 0)
        assertEquals(CardMeta.MetaParts(author = "Katherine Addison", duration = null), parts)
    }

    @Test
    fun `nothing known drops the whole row`() {
        assertNull(CardMeta.metaParts(null, 0))
        assertNull(CardMeta.metaParts("  ", -1))
    }

    // ---- percentLabel ----

    @Test
    fun `started fraction reads as a rounded percent`() {
        assertEquals("25%", CardMeta.percentLabel(0.25))
        assertEquals("34%", CardMeta.percentLabel(0.344))
        assertEquals("35%", CardMeta.percentLabel(0.346))
    }

    @Test
    fun `a barely started book claims 1 percent, never 0`() {
        // 0.001 rounds to 0, but a STARTED card saying "0%" contradicts
        // the segment chip that claims it. Floor at 1.
        assertEquals("1%", CardMeta.percentLabel(0.001))
    }

    @Test
    fun `an almost finished book claims 99 percent, never 100`() {
        // "100%" is the finished badge's claim; a STARTED card must stop
        // at 99 even when rounding lands on 100.
        assertEquals("99%", CardMeta.percentLabel(0.9999))
    }

    @Test
    fun `fractions outside the started range carry no percent`() {
        assertNull(CardMeta.percentLabel(0.0))
        assertNull(CardMeta.percentLabel(-0.2))
        assertNull(CardMeta.percentLabel(1.0))
        assertNull(CardMeta.percentLabel(1.5))
    }

    // ---- barFraction ----

    @Test
    fun `fraction is clamped to the unit interval`() {
        assertEquals(0f, CardMeta.barFraction(-0.25), 0f)
        assertEquals(0.5f, CardMeta.barFraction(0.5), 0f)
        assertEquals(1f, CardMeta.barFraction(1.2), 0f)
    }
}
