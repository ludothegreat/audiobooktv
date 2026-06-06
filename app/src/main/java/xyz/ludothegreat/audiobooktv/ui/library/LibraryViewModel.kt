package xyz.ludothegreat.audiobooktv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.data.cache.LibraryCacheStorage
import xyz.ludothegreat.audiobooktv.data.cache.LibraryRefreshBus
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.domain.LibraryBookSource
import javax.inject.Inject

data class LibraryUiState(
    val loading: Boolean = false,
    val books: List<Book> = emptyList(),
    val offline: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryBookSource,
    private val cache: LibraryCacheStorage,
    private val refreshBus: LibraryRefreshBus,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState(loading = true))
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        loadCachedThenRefresh()
        viewModelScope.launch {
            refreshBus.events.collect { refresh() }
        }
    }

    private fun loadCachedThenRefresh() {
        viewModelScope.launch {
            // Show whatever we last knew immediately so the user has something
            // to look at even before the network call completes (or fails).
            val cached = cache.read()
            if (cached.isNotEmpty()) {
                _state.update {
                    it.copy(books = LibrarySorter.sortForGrid(cached), loading = true)
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
                        it.copy(loading = false, books = LibrarySorter.sortForGrid(books), offline = false)
                    }
                }
                .onFailure { t ->
                    val cached = cache.read()
                    _state.update {
                        if (cached.isNotEmpty()) {
                            it.copy(
                                loading = false,
                                books = LibrarySorter.sortForGrid(cached),
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
}
