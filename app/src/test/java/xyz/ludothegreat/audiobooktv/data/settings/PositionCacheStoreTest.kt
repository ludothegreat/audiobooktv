package xyz.ludothegreat.audiobooktv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The single-slot store lost unconfirmed progress whenever a second book was
 * opened before the server came back. These hold the per-book contract that
 * replaced it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PositionCacheStoreTest {

    private fun store() = PositionCacheStore(ApplicationProvider.getApplicationContext())

    /**
     * preferencesDataStore caches one instance per file per classloader, so
     * records written by one test are still there for the next. Clearing up
     * front keeps each case independent of the order they run in.
     */
    @Before
    fun clearStore() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.positionCacheDataStore.edit { it.clear() }
    }

    @Test
    fun `a second book does not evict the first book's unconfirmed position`() = runTest {
        val s = store()
        s.writeDirty("book-a", 1200.0)
        s.writeDirty("book-b", 45.0)

        // This is the case the single slot got wrong: book-a listened to
        // offline, book-b opened before connectivity returned.
        assertEquals(1200.0, s.read("book-a")?.positionSec)
        assertEquals(45.0, s.read("book-b")?.positionSec)
    }

    @Test
    fun `a confirmed position is dropped so nothing is replayed twice`() = runTest {
        val s = store()
        s.writeDirty("book-a", 1200.0)
        s.markClean("book-a")
        assertNull(s.read("book-a"))
    }

    @Test
    fun `confirming one book leaves another book's pending record intact`() = runTest {
        val s = store()
        s.writeDirty("book-a", 1200.0)
        s.writeDirty("book-b", 45.0)
        s.markClean("book-b")

        assertEquals(1200.0, s.read("book-a")?.positionSec)
        assertNull(s.read("book-b"))
    }

    @Test
    fun `a pending record written by the old single-slot format survives the upgrade`() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.positionCacheDataStore.edit { prefs ->
            prefs[stringPreferencesKey("position_item_id")] = "book-legacy"
            prefs[doublePreferencesKey("position_sec")] = 900.0
            prefs[longPreferencesKey("position_recorded_at_ms")] = 1L
            prefs[booleanPreferencesKey("position_dirty")] = true
        }
        assertEquals(900.0, store().read("book-legacy")?.positionSec)
    }

    @Test
    fun `a confirmed record from the old format is not resurrected`() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        context.positionCacheDataStore.edit { prefs ->
            prefs[stringPreferencesKey("position_item_id")] = "book-clean"
            prefs[doublePreferencesKey("position_sec")] = 900.0
            prefs[booleanPreferencesKey("position_dirty")] = false
        }
        assertNull(store().read("book-clean"))
    }

    @Test
    fun `an unknown book reads as nothing pending`() = runTest {
        assertNull(store().read("never-played"))
    }

    @Test
    fun `a later write for the same book replaces the earlier one`() = runTest {
        val s = store()
        s.writeDirty("book-a", 100.0)
        s.writeDirty("book-a", 200.0)
        assertEquals(200.0, s.read("book-a")?.positionSec)
    }

    @Test
    fun `records stay bounded and keep the most recent work`() = runTest {
        val s = store()
        repeat(20) { i ->
            s.writeDirty("book-$i", i.toDouble())
            // Distinct timestamps so the eviction order is deterministic
            // rather than depending on how fast the loop runs.
            Thread.sleep(2)
        }
        // The oldest are evicted; the newest survive.
        assertNull(s.read("book-0"))
        assertEquals(19.0, s.read("book-19")?.positionSec)
    }
}
