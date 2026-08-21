package xyz.ludothegreat.audiobooktv.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverInitialsTest {

    @Test
    fun `two words give two initials`() {
        assertEquals("PM", CoverInitials.from("Project Maven"))
    }

    @Test
    fun `extra words beyond two are ignored`() {
        assertEquals("TL", CoverInitials.from("The Last Wish"))
    }

    @Test
    fun `single word gives one initial`() {
        assertEquals("D", CoverInitials.from("Dune"))
    }

    @Test
    fun `lowercase titles are uppercased`() {
        assertEquals("OB", CoverInitials.from("of blood and bone"))
    }

    @Test
    fun `punctuation-only tokens are skipped`() {
        assertEquals("AW", CoverInitials.from("... And Wisdom"))
    }

    @Test
    fun `leading punctuation inside a word is skipped to its first letter`() {
        assertEquals("QA", CoverInitials.from("'Quotes' Abound"))
    }

    @Test
    fun `digits count as initials`() {
        assertEquals("19", CoverInitials.from("1984 9th-edition"))
    }

    @Test
    fun `blank and symbol-only titles give empty`() {
        assertEquals("", CoverInitials.from(""))
        assertEquals("", CoverInitials.from("   "))
        assertEquals("", CoverInitials.from("&*# !!"))
    }
}
