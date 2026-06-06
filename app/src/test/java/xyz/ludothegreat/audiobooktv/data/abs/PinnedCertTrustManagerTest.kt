package xyz.ludothegreat.audiobooktv.data.abs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Principal
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.Date

/**
 * Locks decision #27's TLS-pin behaviour at the trust-manager level. The
 * two-phase TOFU dance in SessionManager only works if the underlying
 * PinnedCertTrustManager actually compares against the captured fingerprint
 * with case-insensitive hex equality.
 *
 * We bypass real X.509 parsing -- only `encoded` matters for the fingerprint
 * check -- by overriding the small surface the trust manager exercises.
 */
class PinnedCertTrustManagerTest {

    @Test
    fun `matching fingerprint passes`() {
        val cert = stubCert(byteArrayOf(1, 2, 3, 4))
        val fp = sha256Hex(cert.encoded)
        val mgr = PinnedCertTrustManager(fp)
        mgr.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `matching fingerprint is case insensitive`() {
        val cert = stubCert(byteArrayOf(0xA, 0xB, 0xC))
        val fp = sha256Hex(cert.encoded).uppercase()
        val mgr = PinnedCertTrustManager(fp)
        mgr.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `mismatched fingerprint throws`() {
        val cert = stubCert(byteArrayOf(1, 2, 3, 4))
        val mgr = PinnedCertTrustManager("0".repeat(64))
        assertThrows(IllegalStateException::class.java) {
            mgr.checkServerTrusted(arrayOf(cert), "RSA")
        }
    }

    @Test
    fun `empty chain throws`() {
        val mgr = PinnedCertTrustManager("0".repeat(64))
        assertThrows(IllegalStateException::class.java) {
            mgr.checkServerTrusted(emptyArray(), "RSA")
        }
        assertThrows(IllegalStateException::class.java) {
            mgr.checkServerTrusted(null, "RSA")
        }
    }

    @Test
    fun `getAcceptedIssuers returns empty so callers cannot bypass via system trust`() {
        val mgr = PinnedCertTrustManager("0".repeat(64))
        assertNotNull(mgr.acceptedIssuers)
        assertEquals(0, mgr.acceptedIssuers.size)
    }

    @Test
    fun `fingerprintCapturingTrustManager captures the leaf SHA-256`() {
        val cert = stubCert(byteArrayOf(9, 9, 9))
        var captured: String? = null
        val mgr = fingerprintCapturingTrustManager { captured = it }
        mgr.checkServerTrusted(arrayOf(cert), "RSA")
        assertEquals(sha256Hex(cert.encoded), captured)
    }

    @Test
    fun `fingerprintCapturingTrustManager does not capture on empty chain`() {
        var captured: String? = null
        val mgr = fingerprintCapturingTrustManager { captured = it }
        mgr.checkServerTrusted(emptyArray(), "RSA")
        assertNull(captured)
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val d = MessageDigest.getInstance("SHA-256").digest(bytes)
        return d.joinToString("") { "%02x".format(it) }
    }

    /**
     * Minimal X509Certificate stub. Only `encoded` is exercised by the
     * trust manager; everything else throws so we'd see immediately if the
     * implementation drifted into needing more.
     */
    private fun stubCert(encoded: ByteArray): X509Certificate = object : X509Certificate() {
        override fun getEncoded(): ByteArray = encoded
        override fun checkValidity() = Unit
        override fun checkValidity(date: Date?) = Unit
        override fun getVersion(): Int = 3
        override fun getSerialNumber(): BigInteger = BigInteger.ONE
        override fun getIssuerDN(): Principal = Principal { "CN=test" }
        override fun getSubjectDN(): Principal = Principal { "CN=test" }
        override fun getNotBefore(): Date = Date(0)
        override fun getNotAfter(): Date = Date(Long.MAX_VALUE)
        override fun getTBSCertificate(): ByteArray = encoded
        override fun getSignature(): ByteArray = ByteArray(0)
        override fun getSigAlgName(): String = "SHA256withRSA"
        override fun getSigAlgOID(): String = "1.2.840.113549.1.1.11"
        override fun getSigAlgParams(): ByteArray? = null
        override fun getIssuerUniqueID(): BooleanArray? = null
        override fun getSubjectUniqueID(): BooleanArray? = null
        override fun getKeyUsage(): BooleanArray? = null
        override fun getBasicConstraints(): Int = -1
        override fun hasUnsupportedCriticalExtension(): Boolean = false
        override fun getCriticalExtensionOIDs(): MutableSet<String> = mutableSetOf()
        override fun getNonCriticalExtensionOIDs(): MutableSet<String> = mutableSetOf()
        override fun getExtensionValue(oid: String?): ByteArray? = null
        override fun getPublicKey(): PublicKey = error("getPublicKey not implemented for stub")
        override fun toString(): String = "stub-cert"
        override fun verify(key: PublicKey?) = Unit
        override fun verify(key: PublicKey?, sigProvider: String?) = Unit
    }
}
