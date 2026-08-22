package xyz.ludothegreat.audiobooktv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
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

private val Context.positionCacheDataStore by preferencesDataStore(name = "audiobooktv-position-cache")

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
        val raw = store.data.first()[KEY_RECORDS] ?: return emptyMap()
        return runCatching { json.decodeFromString(entryMapSerializer, raw) }.getOrElse { emptyMap() }
    }

    private suspend fun mutate(block: (Map<String, Entry>) -> Map<String, Entry>) {
        store.edit { prefs ->
            val current = prefs[KEY_RECORDS]
                ?.let { runCatching { json.decodeFromString(entryMapSerializer, it) }.getOrNull() }
                ?: emptyMap()
            prefs[KEY_RECORDS] = json.encodeToString(entryMapSerializer, block(current))
        }
    }

    private companion object {
        const val MAX_RECORDS = 16
        val KEY_RECORDS = stringPreferencesKey("position_records")
    }
}
