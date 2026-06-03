package xyz.ludothegreat.audiobooktv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.data.cache.LibraryCache
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.domain.LibraryRepository
import javax.inject.Inject

data class LibraryUiState(
    val loading: Boolean = false,
    val books: List<Book> = emptyList(),
    val offline: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val cache: LibraryCache,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState(loading = true))
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        loadCachedThenRefresh()
    }

    private fun loadCachedThenRefresh() {
        viewModelScope.launch {
            // Show whatever we last knew immediately so the user has something
            // to look at even before the network call completes (or fails).
            val cached = cache.read()
            if (cached.isNotEmpty()) {
                _state.update {
                    it.copy(books = sortForGrid(cached), loading = true)
                }
            }
            refresh()
        }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.fetchBooks() }
                .onSuccess { books ->
                    cache.write(books)
                    _state.update {
                        it.copy(loading = false, books = sortForGrid(books), offline = false)
                    }
                }
                .onFailure { t ->
                    val cached = cache.read()
                    _state.update {
                        if (cached.isNotEmpty()) {
                            it.copy(
                                loading = false,
                                books = sortForGrid(cached),
                                offline = true,
                                error = null,
                            )
                        } else {
                            it.copy(
                                loading = false,
                                offline = true,
                                error = t.message ?: "Failed to load library.",
                            )
                        }
                    }
                }
        }
    }

    private fun sortForGrid(books: List<Book>): List<Book> {
        val inProgress = books.filter { it.lastUpdate > 0 && !it.isFinished }
            .sortedByDescending { it.lastUpdate }
        val others = books.filter { it.lastUpdate == 0L || it.isFinished }
            .sortedWith(compareBy({ it.isFinished }, { (it.author ?: "").lowercase() }, { it.title.lowercase() }))
        return inProgress + others
    }
}
