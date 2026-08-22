package xyz.ludothegreat.audiobooktv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import xyz.ludothegreat.audiobooktv.playback.LocalPositionRecord
import javax.inject.Inject
import javax.inject.Singleton

// internal, not private: DataStore permits exactly one instance per file per
// classloader, so a test that declared its own delegate for this file would
// crash rather than isolate. Tests reuse this one.
internal val Context.positionCacheDataStore by preferencesDataStore(name = "audiobooktv-position-cache")

/**
 * Per-book persistence for playheads that have not been confirmed by the
 * server, consumed by PositionReconciler on load and refresh.
 *
 * This used to be one slot, on the reasoning that only one book can be
 * mid-playback when a process dies. That reasoning misses the case the cache
 * exists for: listen to a book while the server is unreachable, open a
 * different book before connectivity returns, and the second book's syncs
 * overwrite the first book's unconfirmed progress. The listener loses exactly
 * the stretch the cache was meant to protect.
 *
 * Records are keyed by item, and a record is dropped the moment the server
 * confirms that position, because a confirmed position has nothing left to
 * replay. Only unconfirmed work is retained, so the map is bounded by how
 * many books were played without a reachable server rather than by library
 * size. MAX_RECORDS caps the pathological case; the oldest record goes first.
 *
 * Writes ride the existing sync moments (10s interval while playing, the
 * pause flush, every user seek push); nothing here adds a timer or a poll.
 * `writeDirty` lands BEFORE the network attempt, `markClean` only after the
 * server confirmed, so a crash mid-sync leaves a dirty record to replay.
 */
@Singleton
class PositionCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.positionCacheDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val entryMapSerializer = MapSerializer(String.serializer(), Entry.serializer())

    @Serializable
    private data class Entry(
        val itemId: String,
        val positionSec: Double,
        val recordedAtMs: Long,
        val dirty: Boolean,
    )

    /** The unconfirmed record for [itemId], or null when nothing is pending. */
    suspend fun read(itemId: String): LocalPositionRecord? = readAll()[itemId]?.let {
        LocalPositionRecord(
            itemId = it.itemId,
            positionSec = it.positionSec,
            recordedAtMs = it.recordedAtMs,
            dirty = it.dirty,
        )
    }

    suspend fun writeDirty(itemId: String, positionSec: Double) {
        mutate { current ->
            val next = current + (
                itemId to Entry(
                    itemId = itemId,
                    positionSec = positionSec,
                    recordedAtMs = System.currentTimeMillis(),
                    dirty = true,
                )
                )
            if (next.size <= MAX_RECORDS) {
                next
            } else {
                // Oldest first: the most recent unconfirmed work is the most
                // likely to still matter to the listener.
                next.entries
                    .sortedByDescending { it.value.recordedAtMs }
                    .take(MAX_RECORDS)
                    .associate { it.key to it.value }
            }
        }
    }

    /**
     * The server now holds this position, so there is nothing to replay.
     * Dropping the record rather than flipping a flag is what keeps the map
     * bounded by pending work instead of by listening history.
     */
    suspend fun markClean(itemId: String) {
        mutate { it - itemId }
    }

    private suspend fun readAll(): Map<String, Entry> {
        val prefs = store.data.first()
        val raw = prefs[KEY_RECORDS]
            ?: return legacyRecord(prefs)?.let { mapOf(it.itemId to it) } ?: emptyMap()
        return runCatching { json.decodeFromString(entryMapSerializer, raw) }
            .getOrElse { emptyMap() }
    }

    /**
     * The single-slot format this replaced. Read once so a user upgrading mid
     * offline stretch does not lose the very progress this cache exists to
     * hold; without it the first resume after the update finds nothing pending
     * and the stale server position wins.
     */
    private fun legacyRecord(prefs: Preferences): Entry? {
        val itemId = prefs[LEGACY_ITEM_ID] ?: return null
        val positionSec = prefs[LEGACY_POSITION_SEC] ?: return null
        if (prefs[LEGACY_DIRTY] != true) return null
        return Entry(
            itemId = itemId,
            positionSec = positionSec,
            recordedAtMs = prefs[LEGACY_RECORDED_AT_MS] ?: 0L,
            dirty = true,
        )
    }

    private suspend fun mutate(block: (Map<String, Entry>) -> Map<String, Entry>) {
        store.edit { prefs ->
            val stored = prefs[KEY_RECORDS]
            val current = when {
                stored != null ->
                    runCatching { json.decodeFromString(entryMapSerializer, stored) }.getOrElse { emptyMap() }
                else -> legacyRecord(prefs)?.let { mapOf(it.itemId to it) } ?: emptyMap()
            }
            prefs[KEY_RECORDS] = json.encodeToString(entryMapSerializer, block(current))
            // The legacy slot has been folded in; leaving it would let a later
            // read resurrect a position the new map has already superseded.
            prefs.remove(LEGACY_ITEM_ID)
            prefs.remove(LEGACY_POSITION_SEC)
            prefs.remove(LEGACY_RECORDED_AT_MS)
            prefs.remove(LEGACY_DIRTY)
        }
    }

    private companion object {
        const val MAX_RECORDS = 16
        val KEY_RECORDS = stringPreferencesKey("position_records")

        // Single-slot keys written by builds before this store was per-book.
        val LEGACY_ITEM_ID = stringPreferencesKey("position_item_id")
        val LEGACY_POSITION_SEC = doublePreferencesKey("position_sec")
        val LEGACY_RECORDED_AT_MS = longPreferencesKey("position_recorded_at_ms")
        val LEGACY_DIRTY = booleanPreferencesKey("position_dirty")
    }
}
