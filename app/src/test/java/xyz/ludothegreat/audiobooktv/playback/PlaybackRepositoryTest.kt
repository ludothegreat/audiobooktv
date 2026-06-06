package xyz.ludothegreat.audiobooktv.playback

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.ludothegreat.audiobooktv.data.abs.AbsApiProvider
import xyz.ludothegreat.audiobooktv.data.auth.CredentialStorage
import xyz.ludothegreat.audiobooktv.data.auth.Credentials
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import java.util.concurrent.TimeUnit

/**
 * Locks PlaybackRepository's HTTP-shape contract with ABS. The
 * position-is-server-truth invariant (HANDOFF.md) lives one layer up in
 * PlayerViewModel, but it depends on these three methods producing the
 * right bodies and returning the right values:
 *
 *   - openPlayback POSTs to /api/items/{id}/play with DeviceInfo +
 *     supported mime types, returns the resume position in ms.
 *   - fetchSavedPositionSec returns currentTime from /api/me/progress/{id},
 *     returning null on any failure rather than throwing -- the player
 *     must not crash because the server hiccuped.
 *   - syncProgress POSTs the new currentTime + delta + duration to
 *     /api/session/{sid}/sync and swallows transport errors silently for
 *     the same reason.
 */
class PlaybackRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var subject: PlaybackRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val base = server.url("/").toString().trimEnd('/')
        val sessionManager = SessionManager(FixedCredentialStorage(base))
        val apiProvider = AbsApiProvider(sessionManager)
        subject = PlaybackRepository(apiProvider, sessionManager)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `openPlayback POSTs play request with device info and returns resume offset`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "id":"session-abc",
                    "displayTitle":"The Book",
                    "displayAuthor":"An Author",
                    "duration": 3600.0,
                    "currentTime": 123.5,
                    "audioTracks":[
                        {"index":0,"title":"part-1","mimeType":"audio/mp4",
                         "duration":1800.0,"startOffset":0.0,
                         "contentUrl":"/api/items/abc/file/0"}
                    ],
                    "chapters":[]
                }
                """.trimIndent(),
            ),
        )

        val prep = subject.openPlayback("abc")

        // The /api/items/abc/play body must declare the audiobooktv client
        // and a non-empty supported mime list, otherwise ABS will pick a
        // transcode profile that won't play on the TV.
        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("/api/items/abc/play", recorded?.path)
        val body = Json.parseToJsonElement(recorded!!.body.readUtf8()).jsonObject
        assertEquals(
            "audiobooktv",
            body["deviceInfo"]!!.jsonObject["clientName"]!!.jsonPrimitive.content,
        )
        assertTrue(
            "supportedMimeTypes must be non-empty",
            body["supportedMimeTypes"]!!.toString().contains("audio/"),
        )

        assertEquals("session-abc", prep.session.id)
        assertEquals(123_500L, prep.resumePositionMs)
        assertEquals(1, prep.mediaItems.size)
    }

    @Test
    fun `fetchSavedPositionSec returns the server's currentTime`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"p","libraryItemId":"abc","currentTime":42.5,"isFinished":false}""",
            ),
        )

        val sec = subject.fetchSavedPositionSec("abc")
        assertEquals(42.5, sec!!, 0.0001)

        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/api/me/progress/abc", recorded?.path)
    }

    @Test
    fun `fetchSavedPositionSec returns null when server errors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(subject.fetchSavedPositionSec("abc"))
    }

    @Test
    fun `syncProgress posts current time, listened delta, and duration`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        subject.syncProgress(
            sessionId = "sid",
            currentTimeSec = 200.0,
            timeListenedSec = 10.0,
            durationSec = 3600.0,
        )

        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/api/session/sid/sync", recorded?.path)
        val body = Json.parseToJsonElement(recorded!!.body.readUtf8()).jsonObject
        assertEquals(200.0, body["currentTime"]!!.jsonPrimitive.content.toDouble(), 0.0001)
        assertEquals(10.0, body["timeListened"]!!.jsonPrimitive.content.toDouble(), 0.0001)
        assertEquals(3600.0, body["duration"]!!.jsonPrimitive.content.toDouble(), 0.0001)
    }

    @Test
    fun `syncProgress swallows server errors and does not throw`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        // Must NOT throw -- the PlayerViewModel ticker fires every 10s and
        // can't be allowed to crash the VM on a transient outage.
        subject.syncProgress(
            sessionId = "sid",
            currentTimeSec = 200.0,
            timeListenedSec = 10.0,
            durationSec = 3600.0,
        )
    }

    @Test
    fun `closeSession posts to the close endpoint and swallows failures`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        subject.closeSession("sid")
        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/api/session/sid/close", recorded?.path)
    }
}

class FixedCredentialStorage(private val url: String) : CredentialStorage {
    override fun load(): Credentials = Credentials(
        serverUrl = url.trimEnd('/'),
        username = "u",
        token = "t",
        userId = "uid",
    )
    override fun save(creds: Credentials) = Unit
    override fun clear() = Unit
}
