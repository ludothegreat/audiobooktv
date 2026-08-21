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
import retrofit2.HttpException
import xyz.ludothegreat.audiobooktv.data.abs.AbsApiProvider
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import java.util.concurrent.TimeUnit

/**
 * Locks BookmarksRepository's ABS contract, verified against a live 2.35.1
 * server: bookmarks carry no server UUID, only libraryItemId + time + title +
 * createdAt, so createdAt becomes the domain id. Fetch filters to the current
 * item and sorts by time; create/rename round-trip one bookmark; rename is a
 * PATCH addressed by body.time; delete is addressed by the time path segment
 * and treats 404 as already-deleted success.
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
                        {"libraryItemId":"other","time":42.0,"title":"skip me","createdAt":1780526584001},
                        {"libraryItemId":"abc","time":120.0,"title":"chapter B","createdAt":1780526584002},
                        {"libraryItemId":"abc","time":30.0,"title":"chapter A","createdAt":1780526584003},
                        {"libraryItemId":"abc","time":250.0,"title":"chapter C","createdAt":1780526584004}
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
    fun `fetchForItem populates the id from createdAt`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "bookmarks":[
                        {"libraryItemId":"abc","time":130,"title":"2:10","createdAt":1780526584570}
                    ]
                }
                """.trimIndent(),
            ),
        )
        assertEquals("1780526584570", subject.fetchForItem("abc").single().id)
    }

    @Test
    fun `fetchForItem returns empty list on server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(subject.fetchForItem("abc").isEmpty())
    }

    @Test
    fun `fetchForItem handles missing title and createdAt gracefully`() = runTest {
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
        assertNull(result[0].id)
    }

    @Test
    fun `create posts bookmark with timestamp seconds and label`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"libraryItemId":"abc","time":90.0,"title":"1:30","createdAt":1787288043916}""",
            ),
        )

        val created = subject.create("abc", timeSec = 90, title = "1:30")
        assertNotNull(created)
        assertEquals(90L, created?.timeSec)
        assertEquals("1:30", created?.title)
        assertEquals("1787288043916", created?.id)

        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("POST", recorded?.method)
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

    @Test
    fun `rename patches the bookmark addressed by its time`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"libraryItemId":"abc","time":4321,"title":"renamed","createdAt":1787288043916}""",
            ),
        )

        val result = subject.rename("abc", timeSec = 4321, title = "renamed")

        val renamed = result.getOrThrow()
        assertEquals(4321L, renamed.timeSec)
        assertEquals("renamed", renamed.title)
        // Rename preserves createdAt server-side, so the id survives.
        assertEquals("1787288043916", renamed.id)

        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("PATCH", recorded?.method)
        assertEquals("/api/me/item/abc/bookmark", recorded?.path)
        val body = Json.parseToJsonElement(recorded!!.body.readUtf8()).jsonObject
        assertEquals(4321.0, body["time"]!!.jsonPrimitive.content.toDouble(), 0.0001)
        assertEquals("renamed", body["title"]!!.jsonPrimitive.content)
    }

    @Test
    fun `rename fails loudly when the bookmark is gone`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        val result = subject.rename("abc", timeSec = 999, title = "x")
        assertTrue(result.isFailure)
        assertEquals(404, (result.exceptionOrNull() as HttpException).code())
    }

    @Test
    fun `rename fails loudly on server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(subject.rename("abc", timeSec = 90, title = "x").isFailure)
    }

    @Test
    fun `delete removes the bookmark addressed by the time path segment`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        val result = subject.delete("abc", timeSec = 4321)

        assertTrue(result.isSuccess)
        val recorded = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("DELETE", recorded?.method)
        assertEquals("/api/me/item/abc/bookmark/4321", recorded?.path)
    }

    @Test
    fun `delete treats 404 as already deleted`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))
        assertTrue(subject.delete("abc", timeSec = 4321).isSuccess)
    }

    @Test
    fun `delete fails loudly on server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(subject.delete("abc", timeSec = 4321).isFailure)
    }
}
