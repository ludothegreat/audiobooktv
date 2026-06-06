package xyz.ludothegreat.audiobooktv.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks decision #25's ABS version gate. Pre-releases of the same base
 * (`2.20.0-beta`) compare as the base, not as something older.
 */
class VersionGateTest {

    @Test
    fun `minimum version is supported`() {
        assertTrue(VersionGate.isSupported("2.20.0"))
    }

    @Test
    fun `version with v prefix is supported`() {
        assertTrue(VersionGate.isSupported("v2.20.0"))
    }

    @Test
    fun `older patch is rejected`() {
        assertFalse(VersionGate.isSupported("2.19.9"))
    }

    @Test
    fun `older minor is rejected`() {
        assertFalse(VersionGate.isSupported("2.19.0"))
        assertFalse(VersionGate.isSupported("1.99.99"))
    }

    @Test
    fun `newer minor is supported`() {
        assertTrue(VersionGate.isSupported("2.21.0"))
        assertTrue(VersionGate.isSupported("2.35.1"))
    }

    @Test
    fun `pre-release suffix compares as the base`() {
        assertTrue(VersionGate.isSupported("2.20.0-beta"))
        assertTrue(VersionGate.isSupported("2.20.0-rc1"))
        assertFalse(VersionGate.isSupported("2.19.0-rc1"))
    }

    @Test
    fun `garbled version is rejected`() {
        assertFalse(VersionGate.isSupported(""))
        assertFalse(VersionGate.isSupported("not-a-version"))
        assertFalse(VersionGate.isSupported("abc.def.ghi"))
    }

    @Test
    fun `whitespace tolerated around version`() {
        assertTrue(VersionGate.isSupported(" 2.20.0 "))
        assertTrue(VersionGate.isSupported("  v2.21.0  "))
    }

    @Test
    fun `shorter version pads with zero`() {
        // Both "2.20" and "2.20.0" should compare equal to minimum "2.20.0".
        assertTrue(VersionGate.isSupported("2.20"))
        assertFalse(VersionGate.isSupported("2.19"))
    }
}
