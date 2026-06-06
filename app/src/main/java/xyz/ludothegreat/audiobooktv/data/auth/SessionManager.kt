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
    private val store: CredentialStorage,
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

        val isHttps = serverUrl.trim().startsWith("https://", ignoreCase = true)
        val shouldPin = trustSelfSignedCert && isHttps

        return runCatching {
            // Phase 1: capture the cert fingerprint over a request that carries
            // NO credentials. Even if a MITM is in the path, only /ping leaks.
            // Only meaningful for https:// — http:// has no TLS handshake, so the
            // trust toggle is silently ignored when scheme is plaintext.
            val pinnedFingerprint: String? = if (shouldPin) {
                var captured: String? = null
                val probe = AbsClientFactory.build(
                    AbsTarget(baseUrl = serverUrl, trustCert = TrustMode.Capture { captured = it }),
                )
                probe.ping().also {
                    if (it.success != true) error("server did not return success on /ping")
                }
                captured ?: error("could not capture server certificate fingerprint")
            } else {
                null
            }

            // Phase 2: rebuild with the appropriate trust mode and send the rest
            // (including the password) only after the cert is locked down.
            val verifiedTrust: TrustMode = pinnedFingerprint?.let { TrustMode.Pinned(it) } ?: TrustMode.Strict
            val client = AbsClientFactory.build(AbsTarget(baseUrl = serverUrl, trustCert = verifiedTrust))

            if (pinnedFingerprint == null) {
                // Strict mode: still ping to surface unreachable-server errors early.
                client.ping().also {
                    if (it.success != true) error("server did not return success on /ping")
                }
            }
            val status = client.status()
            val version = status.serverVersion
                ?: return ConnectResult.Failure("Server did not report a version.")
            if (!VersionGate.isSupported(version)) {
                return ConnectResult.Failure(
                    "Audiobookshelf $version is not supported. " +
                        "Need ${VersionGate.MIN_SERVER_VERSION} or newer.",
                )
            }
            val login = client.login(LoginRequest(username = username, password = password))
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
                pinnedCertSha256 = pinnedFingerprint,
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
        // Mirror VersionGate.MIN_SERVER_VERSION at the SessionManager level
        // for callers that already reach for SessionManager.MIN_SERVER_VERSION
        // (HANDOFF.md / README cross-reference this name).
        const val MIN_SERVER_VERSION: String = VersionGate.MIN_SERVER_VERSION
    }
}
