package xyz.ludothegreat.audiobooktv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import xyz.ludothegreat.audiobooktv.playback.LocalPositionRecord
import javax.inject.Inject
import javax.inject.Singleton

private val Context.positionCacheDataStore by preferencesDataStore(name = "audiobooktv-position-cache")

/**
 * Single-slot persistence for the playhead of the book currently in the
 * player, consumed by PositionReconciler on load and refresh. One slot is
 * deliberate: the record exists to survive a process death, and only one
 * book can be mid-playback when the process dies. Loading a different book
 * starts overwriting the slot with that book's positions.
 *
 * Writes ride the existing sync moments (10s interval while playing, the
 * pause flush, every user seek push); nothing here adds a timer or a poll.
 * `writeDirty` lands BEFORE the network attempt, `markClean` only after the
 * server confirmed, so a crash mid-sync leaves a dirty record and the
 * reconciler can replay it.
 */
@Singleton
class PositionCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.positionCacheDataStore

    suspend fun read(): LocalPositionRecord? {
        val prefs = store.data.first()
        val itemId = prefs[KEY_ITEM_ID] ?: return null
        val positionSec = prefs[KEY_POSITION_SEC] ?: return null
        return LocalPositionRecord(
            itemId = itemId,
            positionSec = positionSec,
            recordedAtMs = prefs[KEY_RECORDED_AT_MS] ?: 0L,
            dirty = prefs[KEY_DIRTY] ?: false,
        )
    }

    suspend fun writeDirty(itemId: String, positionSec: Double) = write(itemId, positionSec, dirty = true)

    suspend fun markClean(itemId: String, positionSec: Double) = write(itemId, positionSec, dirty = false)

    private suspend fun write(itemId: String, positionSec: Double, dirty: Boolean) {
        store.edit { prefs ->
            prefs[KEY_ITEM_ID] = itemId
            prefs[KEY_POSITION_SEC] = positionSec
            prefs[KEY_RECORDED_AT_MS] = System.currentTimeMillis()
            prefs[KEY_DIRTY] = dirty
        }
    }

    private companion object {
        val KEY_ITEM_ID = stringPreferencesKey("position_item_id")
        val KEY_POSITION_SEC = doublePreferencesKey("position_sec")
        val KEY_RECORDED_AT_MS = longPreferencesKey("position_recorded_at_ms")
        val KEY_DIRTY = booleanPreferencesKey("position_dirty")
    }
}
