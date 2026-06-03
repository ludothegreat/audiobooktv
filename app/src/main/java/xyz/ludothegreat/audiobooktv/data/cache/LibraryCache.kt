package xyz.ludothegreat.audiobooktv.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.ludothegreat.audiobooktv.domain.Book
import javax.inject.Inject
import javax.inject.Singleton

private val Context.libraryCacheStore by preferencesDataStore(name = "audiobooktv-library-cache")

@Serializable
private data class CachedBook(
    val id: String,
    val title: String,
    val author: String?,
    val series: String?,
    val coverUrl: String?,
    val durationSec: Long,
    val numChapters: Int,
    val progressFraction: Double,
    val isFinished: Boolean,
    val lastUpdate: Long,
)

@Singleton
class LibraryCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.libraryCacheStore
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(): List<Book> {
        val raw = store.data.map { it[KEY_BOOKS] }.first() ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<CachedBook>>(raw).map { it.toBook() }
        }.getOrDefault(emptyList())
    }

    suspend fun write(books: List<Book>) {
        val encoded = json.encodeToString(books.map { it.toCached() })
        store.edit { it[KEY_BOOKS] = encoded }
    }

    suspend fun clear() {
        store.edit { it.remove(KEY_BOOKS) }
    }

    private fun Book.toCached() = CachedBook(
        id, title, author, series, coverUrl, durationSec, numChapters,
        progressFraction, isFinished, lastUpdate,
    )

    private fun CachedBook.toBook() = Book(
        id, title, author, series, coverUrl, durationSec, numChapters,
        progressFraction, isFinished, lastUpdate,
    )

    companion object {
        private val KEY_BOOKS = stringPreferencesKey("books_json")
    }
}
