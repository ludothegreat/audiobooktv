package xyz.ludothegreat.audiobooktv.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the ""-means-missing normalization at the metadata edge. ABS's
 * minified metadata answers with authorName = "" (not null) for an
 * untagged book, measured against Audiobookshelf 2.35.1.
 */
class MetadataFieldTest {

    @Test
    fun `null stays null`() {
        assertNull(metadataField(null))
    }

    @Test
    fun `empty string reads as missing`() {
        assertNull(metadataField(""))
    }

    @Test
    fun `whitespace-only reads as missing`() {
        assertNull(metadataField("   "))
    }

    @Test
    fun `real value is trimmed`() {
        assertEquals("Joe Abercrombie", metadataField(" Joe Abercrombie "))
    }

    @Test
    fun `clean value passes through unchanged`() {
        assertEquals("Katherine Addison", metadataField("Katherine Addison"))
    }
}
