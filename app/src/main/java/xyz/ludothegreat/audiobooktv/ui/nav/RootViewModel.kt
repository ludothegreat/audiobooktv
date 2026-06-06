package xyz.ludothegreat.audiobooktv.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.domain.LibraryBookSource
import javax.inject.Inject

data class InitialActive(
    val bookId: String,
    val coverUrl: String?,
)

data class RootUiState(
    val resolved: Boolean = false,
    val initial: InitialActive? = null,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val libraryRepository: LibraryBookSource,
) : ViewModel() {

    private val _state = MutableStateFlow(RootUiState())
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = runCatching { libraryRepository.fetchInProgress() }
                .getOrDefault(emptyList())
                .firstOrNull()
                ?.let { book: Book -> InitialActive(book.id, book.coverUrl) }
            _state.update { it.copy(resolved = true, initial = initial) }
        }
    }
}
