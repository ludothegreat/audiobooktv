package xyz.ludothegreat.audiobooktv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the seriesName string parsing that feeds the card labels. The ABS
 * minified item gives one string ("Name #4", multi-series joined with
 * ", "); these tests pin how that string becomes "4. Title" plus a series
 * line, including the deliberately-permissive fallbacks.
 */
class SeriesLabelTest {

    @Test
    fun `name and sequence split at the hash`() {
        val parsed = SeriesLabel.parse("Dungeon Crawler Carl #4")
        assertEquals("Dungeon Crawler Carl", parsed?.name)
        assertEquals("4", parsed?.sequence)
    }

    @Test
    fun `decimal sequences survive`() {
        val parsed = SeriesLabel.parse("The Expanse #1.5")
        assertEquals("The Expanse", parsed?.name)
        assertEquals("1.5", parsed?.sequence)
    }

    @Test
    fun `series without a sequence keeps the name only`() {
        val parsed = SeriesLabel.parse("Standalone Collection")
        assertEquals("Standalone Collection", parsed?.name)
        assertNull(parsed?.sequence)
    }

    @Test
    fun `null and blank input parse to null`() {
        assertNull(SeriesLabel.parse(null))
        assertNull(SeriesLabel.parse(""))
        assertNull(SeriesLabel.parse("   "))
    }

    @Test
    fun `multi-series string uses the first entry with a sequence`() {
        val parsed = SeriesLabel.parse("Cradle #7, Willverse #12")
        assertEquals("Cradle", parsed?.name)
        assertEquals("7", parsed?.sequence)
    }

    @Test
    fun `entry without a hash is skipped in favour of one with a sequence`() {
        val parsed = SeriesLabel.parse("Loose Collection, Cradle #7")
        assertEquals("Cradle", parsed?.name)
        assertEquals("7", parsed?.sequence)
    }

    @Test
    fun `non-numeric sequence is not shown as a number`() {
        val parsed = SeriesLabel.parse("Mystery Series #TBD")
        assertEquals("Mystery Series #TBD", parsed?.name)
        assertNull(parsed?.sequence)
    }

    @Test
    fun `numberedTitle prefixes the sequence`() {
        assertEquals(
            "4. The Butcher's Masquerade",
            SeriesLabel.numberedTitle("The Butcher's Masquerade", "Dungeon Crawler Carl #4"),
        )
    }

    @Test
    fun `numberedTitle without a sequence leaves the title alone`() {
        assertEquals("Solo Book", SeriesLabel.numberedTitle("Solo Book", null))
        assertEquals("Solo Book", SeriesLabel.numberedTitle("Solo Book", "Loose Collection"))
    }

    @Test
    fun `seriesLine is the parsed name or null`() {
        assertEquals("Cradle", SeriesLabel.seriesLine("Cradle #7"))
        assertEquals("Loose Collection", SeriesLabel.seriesLine("Loose Collection"))
        assertNull(SeriesLabel.seriesLine(null))
        assertNull(SeriesLabel.seriesLine(" "))
    }
}
