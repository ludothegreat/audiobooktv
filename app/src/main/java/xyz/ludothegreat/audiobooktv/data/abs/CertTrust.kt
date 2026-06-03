package xyz.ludothegreat.audiobooktv.data.abs

import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class PinnedCertTrustManager(
    private val expectedSha256Hex: String,
) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: error("empty server certificate chain")
        val actual = sha256Hex(leaf.encoded)
        if (!actual.equals(expectedSha256Hex, ignoreCase = true)) {
            error("server certificate fingerprint mismatch: expected $expectedSha256Hex, got $actual")
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

class AcceptAllTrustManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

fun fingerprintCapturingTrustManager(onCapture: (String) -> Unit): X509TrustManager =
    object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            val leaf = chain?.firstOrNull() ?: return
            onCapture(sha256Hex(leaf.encoded))
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

internal fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}
