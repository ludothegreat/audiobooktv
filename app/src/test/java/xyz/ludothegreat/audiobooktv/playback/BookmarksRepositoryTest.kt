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
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import java.util.concurrent.TimeUnit

/**
 * Locks BookmarksRepository's ABS contract: fetch is filtered to the
 * current item then sorted by time, and create round-trips a single new
 * bookmark with timestamp + label. Errors degrade gracefully so the
 * panel renders "no bookmarks yet" instead of crashing the player.
 */
class BookmarksRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var subject: BookmarksRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val base = server.url("/").toString().trimEnd('/')
        val sessionManager = SessionManager(FixedCredentialStorage(base))
        subject = BookmarksRepository(AbsApiProvider(sessionManager))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchForItem returns only matching bookmarks sorted by time`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "id":"user-1",
                    "username":"u",
                    "bookmarks":[
                        {"libraryItemId":"other","time":42.0,"title":"skip me"},
                        {"libraryItemId":"abc","time":120.0,"title":"chapter B"},
                        {"libraryItemId":"abc","time":30.0,"title":"chapter A"},
                        {"libraryItemId":"abc","time":250.0,"title":"chapter C"}
                    ]
                }
                """.trimIndent(),
            ),
        )

        val result = subject.fetchForItem("abc")

        assertEquals(3, result.size)
        assertEquals("chapter A", result[0].title)
        assertEquals(30L, result[0].timeSec)
        assertEquals("chapter B", result[1].title)
        assertEquals("chapter C", result[2].title)
    }

    @Test
    fun `fetchForItem returns empty list on server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(subject.fetchForItem("abc").isEmpty())
    }

    @Test
    fun `fetchForItem handles missing title gracefully`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "bookmarks":[
                        {"libraryItemId":"abc","time":5.0}
                    ]
                }
                """.trimIndent(),
            ),
        )
        val result = subject.fetchForItem("abc")
        assertEquals(1, result.size)
        assertEquals("", result[0].title)
    }

    @Test
    fun `create posts bookmark with timestamp seconds and label`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"libraryItemId":"abc","time":90.0,"title":"1:30"}""",
            ),
        )

        val created = subject.create("abc", timeSec = 90, title = "1:30")
        assertNotNull(created)
        assertEquals(90L, created?.timeSec)
        assertEquals("1:30", created?.title)

        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("/api/me/item/abc/bookmark", recorded?.path)
        val body = Json.parseToJsonElement(recorded!!.body.readUtf8()).jsonObject
        assertEquals(90.0, body["time"]!!.jsonPrimitive.content.toDouble(), 0.0001)
        assertEquals("1:30", body["title"]!!.jsonPrimitive.content)
    }

    @Test
    fun `create returns null when server rejects the request`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(subject.create("abc", timeSec = 100, title = "x"))
    }
}
