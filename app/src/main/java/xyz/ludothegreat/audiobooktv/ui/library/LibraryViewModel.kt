package xyz.ludothegreat.audiobooktv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.domain.LibraryRepository
import javax.inject.Inject

data class LibraryUiState(
    val loading: Boolean = false,
    val books: List<Book> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState(loading = true))
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.fetchBooks() }
                .onSuccess { books ->
                    _state.update { it.copy(loading = false, books = sortForGrid(books)) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, error = t.message ?: "Failed to load library.") }
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
