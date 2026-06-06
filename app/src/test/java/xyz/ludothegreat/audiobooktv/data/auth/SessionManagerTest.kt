package xyz.ludothegreat.audiobooktv.data.auth

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.ludothegreat.audiobooktv.testutil.MainDispatcherRule
import java.util.concurrent.TimeUnit

/**
 * Locks the SessionManager.connect happy and unhappy paths over plain HTTP,
 * which is the path the app actually uses on the test Onn box. The HTTPS pin
 * flow's trust-manager wiring is covered by [PinnedCertTrustManagerTest];
 * here we focus on the request order and the version gate which are the two
 * places where a regression silently drops the password on the wire ahead of
 * verification.
 */
class SessionManagerTest {

    @get:org.junit.Rule
    val mainRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var store: InMemoryCredentialStorage
    private lateinit var subject: SessionManager

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        store = InMemoryCredentialStorage()
        subject = SessionManager(store)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `connect succeeds and saves credentials when server version is supported`() = runTest {
        enqueuePing(success = true)
        enqueueStatus(version = "2.20.0")
        enqueueLogin(username = "alice", token = "jwt-token", userId = "user-1")

        val result = subject.connect(
            serverUrl = server.url("/").toString().trimEnd('/'),
            username = "alice",
            password = "secret",
            trustSelfSignedCert = false,
        )

        assertTrue("expected Ok, got $result", result is ConnectResult.Ok)
        val saved = store.load()
        assertNotNull(saved)
        assertEquals("jwt-token", saved?.token)
        assertEquals("user-1", saved?.userId)
        // Credentials.username comes from the login response, mirroring whatever
        // canonical form the server uses for the account.
        assertEquals("alice", saved?.username)
        assertNull("HTTP connect must not pin a cert", saved?.pinnedCertSha256)
    }

    @Test
    fun `connect calls ping then status then login in that order`() = runTest {
        enqueuePing(success = true)
        enqueueStatus(version = "2.20.0")
        enqueueLogin(username = "u", token = "t", userId = "u")

        subject.connect(
            serverUrl = server.url("/").toString().trimEnd('/'),
            username = "u",
            password = "p",
            trustSelfSignedCert = false,
        )

        val first = server.takeRequest(2, TimeUnit.SECONDS)
        val second = server.takeRequest(2, TimeUnit.SECONDS)
        val third = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/ping", first?.path)
        assertEquals("/status", second?.path)
        assertEquals("/login", third?.path)
    }

    @Test
    fun `connect rejects older server versions before sending the password`() = runTest {
        enqueuePing(success = true)
        enqueueStatus(version = "2.19.9")
        // No login response enqueued -- if SessionManager calls /login, the
        // server would return 404 and the test catches it.

        val result = subject.connect(
            serverUrl = server.url("/").toString().trimEnd('/'),
            username = "alice",
            password = "must-not-leak",
            trustSelfSignedCert = false,
        )

        assertTrue("expected Failure for old version, got $result", result is ConnectResult.Failure)
        val first = server.takeRequest(2, TimeUnit.SECONDS)
        val second = server.takeRequest(2, TimeUnit.SECONDS)
        val third = server.takeRequest(500, TimeUnit.MILLISECONDS)
        assertEquals("/ping", first?.path)
        assertEquals("/status", second?.path)
        assertNull("must not call /login after version gate fails", third)
        assertNull("must not persist credentials on failure", store.load())
    }

    @Test
    fun `connect propagates the login error message verbatim`() = runTest {
        enqueuePing(success = true)
        enqueueStatus(version = "2.20.0")
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"error":"invalid username or password"}"""),
        )

        val result = subject.connect(
            serverUrl = server.url("/").toString().trimEnd('/'),
            username = "alice",
            password = "wrong",
            trustSelfSignedCert = false,
        )

        assertTrue(result is ConnectResult.Failure)
        val message = (result as ConnectResult.Failure).message
        assertEquals("invalid username or password", message)
        assertNull(store.load())
    }

    @Test
    fun `connect fails fast on blank input without touching the network`() = runTest {
        val blanks = listOf("", "u", "p")
        listOf(
            Triple("", "user", "pass"),
            Triple("http://x", "", "pass"),
            Triple("http://x", "user", ""),
        ).forEachIndexed { i, (u, n, p) ->
            val result = subject.connect(u, n, p, trustSelfSignedCert = false)
            assertTrue("input $i should fail validation: $blanks", result is ConnectResult.Failure)
        }
        assertEquals("no network calls expected on blank input", 0, server.requestCount)
    }

    @Test
    fun `logout clears the persisted credentials`() = runTest {
        enqueuePing(success = true)
        enqueueStatus(version = "2.20.0")
        enqueueLogin(username = "u", token = "t", userId = "u")
        subject.connect(
            serverUrl = server.url("/").toString().trimEnd('/'),
            username = "u",
            password = "p",
            trustSelfSignedCert = false,
        )
        assertNotNull(store.load())
        subject.logout()
        assertNull(store.load())
    }

    private fun enqueuePing(success: Boolean) {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":$success}"""),
        )
    }

    private fun enqueueStatus(version: String) {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"serverVersion":"$version","isInit":true}"""),
        )
    }

    private fun enqueueLogin(username: String, token: String, userId: String) {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(
                    """{"user":{"id":"$userId","username":"$username","token":"$token","type":"user"}}""",
                ),
        )
    }
}

class InMemoryCredentialStorage : CredentialStorage {
    private var stored: Credentials? = null
    override fun load(): Credentials? = stored
    override fun save(creds: Credentials) {
        stored = creds
    }
    override fun clear() {
        stored = null
    }
}
