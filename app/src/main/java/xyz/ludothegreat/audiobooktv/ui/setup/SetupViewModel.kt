package xyz.ludothegreat.audiobooktv.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.data.auth.ConnectResult
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import javax.inject.Inject

data class SetupUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val trustSelfSignedCert: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onUrlChange(value: String) = _state.update { it.copy(serverUrl = value, error = null) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onTrustToggle(value: Boolean) = _state.update { it.copy(trustSelfSignedCert = value, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = sessionManager.connect(
                serverUrl = current.serverUrl,
                username = current.username,
                password = current.password,
                trustSelfSignedCert = current.trustSelfSignedCert,
            )
            when (result) {
                is ConnectResult.Ok -> {
                    _state.update { it.copy(submitting = false) }
                    onSuccess()
                }
                is ConnectResult.Failure -> {
                    _state.update { it.copy(submitting = false, error = result.message) }
                }
            }
        }
    }
}
