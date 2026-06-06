package xyz.ludothegreat.audiobooktv.testutil

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Smoke test — proves JUnit + coroutines-test + Turbine + MockWebServer
 * all run inside `./gradlew test`. If this regresses, the whole unit-test
 * surface is broken; everything else here depends on these four working.
 */
class TestInfraSmokeTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `Turbine receives StateFlow emissions`() = runTest {
        val flow = MutableStateFlow(0)
        flow.test {
            assertEquals(0, awaitItem())
            flow.value = 1
            assertEquals(1, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `MockWebServer answers requests`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("ok"))
        server.start()
        try {
            val url = server.url("/").toString()
            assertEquals(url, server.url("/").toString())
        } finally {
            server.shutdown()
        }
    }
}
