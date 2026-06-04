package xyz.ludothegreat.audiobooktv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import xyz.ludothegreat.audiobooktv.data.cache.LibraryRefreshBus
import xyz.ludothegreat.audiobooktv.data.settings.AppSettings
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val username: String = "",
    val stopOnAppClose: Boolean = false,
    val diagnosticLogEnabled: Boolean = false,
    val refreshFeedback: String? = null,
    val versionName: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val appSettings: AppSettings,
    private val refreshBus: LibraryRefreshBus,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        val creds = sessionManager.state.value
        _state.update {
            it.copy(
                serverUrl = creds?.serverUrl ?: "",
                username = creds?.username ?: "",
                versionName = xyz.ludothegreat.audiobooktv.BuildConfig.VERSION_NAME,
            )
        }
        viewModelScope.launch {
            appSettings.stopOnAppClose.collect { v ->
                _state.update { it.copy(stopOnAppClose = v) }
            }
        }
        viewModelScope.launch {
            appSettings.diagnosticLogEnabled.collect { v ->
                _state.update { it.copy(diagnosticLogEnabled = v) }
            }
        }
    }

    fun setStopOnAppClose(value: Boolean) {
        viewModelScope.launch { appSettings.setStopOnAppClose(value) }
    }

    fun setDiagnosticLogEnabled(value: Boolean) {
        viewModelScope.launch { appSettings.setDiagnosticLogEnabled(value) }
    }

    fun refreshLibrary() {
        refreshBus.request()
        _state.update { it.copy(refreshFeedback = "Library refresh requested.") }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2_000)
            _state.update { it.copy(refreshFeedback = null) }
        }
    }

    fun logout() {
        sessionManager.logout()
    }
}
