package xyz.ludothegreat.audiobooktv.data.abs

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class AbsTarget(
    val baseUrl: String,
    val token: String? = null,
    val trustCert: TrustMode = TrustMode.Strict,
)

sealed class TrustMode {
    object Strict : TrustMode()
    object AcceptAll : TrustMode()
    data class Pinned(val sha256Hex: String) : TrustMode()
    data class Capture(val onCapture: (String) -> Unit) : TrustMode()
}

object AbsClientFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun build(target: AbsTarget): AbsApi = retrofit(target).create(AbsApi::class.java)

    fun retrofit(target: AbsTarget): Retrofit =
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(target.baseUrl))
            .client(okHttp(target))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun okHttp(target: AbsTarget): OkHttpClient {
        val builder = OkHttpClient.Builder()
        target.token?.takeIf { it.isNotBlank() }?.let { builder.addInterceptor(authInterceptor(it)) }
        when (val mode = target.trustCert) {
            TrustMode.Strict -> Unit
            TrustMode.AcceptAll -> applyTrust(builder, AcceptAllTrustManager())
            is TrustMode.Pinned -> applyTrust(builder, PinnedCertTrustManager(mode.sha256Hex))
            is TrustMode.Capture -> applyTrust(builder, fingerprintCapturingTrustManager(mode.onCapture))
        }
        return builder.build()
    }

    private fun applyTrust(builder: OkHttpClient.Builder, manager: X509TrustManager) {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf(manager), SecureRandom())
        builder.sslSocketFactory(ctx.socketFactory, manager)
        builder.hostnameVerifier { _, _ -> true }
    }

    private fun authInterceptor(token: String) = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        chain.proceed(req)
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
