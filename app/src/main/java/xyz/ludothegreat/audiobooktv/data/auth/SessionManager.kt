package xyz.ludothegreat.audiobooktv.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.ludothegreat.audiobooktv.data.abs.AbsClientFactory
import xyz.ludothegreat.audiobooktv.data.abs.AbsTarget
import xyz.ludothegreat.audiobooktv.data.abs.TrustMode
import xyz.ludothegreat.audiobooktv.data.abs.dto.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

sealed class ConnectResult {
    data class Ok(val credentials: Credentials, val serverVersion: String) : ConnectResult()
    data class Failure(val message: String) : ConnectResult()
}

@Singleton
class SessionManager @Inject constructor(
    private val store: CredentialStore,
) {
    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<Credentials?> = _state

    val isAuthenticated: Boolean get() = _state.value != null

    suspend fun connect(
        serverUrl: String,
        username: String,
        password: String,
        trustSelfSignedCert: Boolean,
    ): ConnectResult {
        if (serverUrl.isBlank()) return ConnectResult.Failure("Server URL is required.")
        if (username.isBlank()) return ConnectResult.Failure("Username is required.")
        if (password.isBlank()) return ConnectResult.Failure("Password is required.")

        var capturedFingerprint: String? = null
        val trust = if (trustSelfSignedCert) {
            TrustMode.Capture { capturedFingerprint = it }
        } else {
            TrustMode.Strict
        }

        val probe = AbsClientFactory.build(AbsTarget(baseUrl = serverUrl, trustCert = trust))

        return runCatching {
            probe.ping().also {
                if (it.success != true) error("server did not return success on /ping")
            }
            val status = probe.status()
            val version = status.serverVersion
                ?: return ConnectResult.Failure("Server did not report a version.")
            val supported = isVersionSupported(version)
            if (!supported) {
                return ConnectResult.Failure(
                    "Audiobookshelf $version is not supported. Need $MIN_SERVER_VERSION or newer.",
                )
            }
            val login = probe.login(LoginRequest(username = username, password = password))
            val user = login.user ?: return ConnectResult.Failure(
                login.error ?: "Login failed.",
            )
            val token = user.token ?: return ConnectResult.Failure(
                "Server did not issue a token.",
            )
            val creds = Credentials(
                serverUrl = serverUrl.trim().trimEnd('/'),
                username = user.username,
                token = token,
                userId = user.id,
                pinnedCertSha256 = if (trustSelfSignedCert) capturedFingerprint else null,
            )
            store.save(creds)
            _state.value = creds
            ConnectResult.Ok(creds, version)
        }.getOrElse { t ->
            ConnectResult.Failure(t.message ?: "Connection failed.")
        }
    }

    fun logout() {
        store.clear()
        _state.value = null
    }

    fun currentTarget(): AbsTarget? {
        val c = _state.value ?: return null
        val mode = c.pinnedCertSha256?.let { TrustMode.Pinned(it) } ?: TrustMode.Strict
        return AbsTarget(baseUrl = c.serverUrl, token = c.token, trustCert = mode)
    }

    companion object {
        const val MIN_SERVER_VERSION = "2.20.0"
        private fun isVersionSupported(version: String): Boolean {
            val v = parseVersion(version) ?: return false
            val min = parseVersion(MIN_SERVER_VERSION) ?: return false
            return compareVersions(v, min) >= 0
        }

        private fun parseVersion(text: String): IntArray? {
            val cleaned = text.removePrefix("v").substringBefore('-')
            val parts = cleaned.split('.')
            if (parts.isEmpty()) return null
            return IntArray(parts.size) { parts[it].toIntOrNull() ?: return null }
        }

        private fun compareVersions(a: IntArray, b: IntArray): Int {
            val n = maxOf(a.size, b.size)
            for (i in 0 until n) {
                val ai = if (i < a.size) a[i] else 0
                val bi = if (i < b.size) b[i] else 0
                if (ai != bi) return ai - bi
            }
            return 0
        }
    }
}
