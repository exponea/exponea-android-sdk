package com.exponea.example.ui.anonymize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exponea.example.model.SdkSetupState
import com.exponea.example.status.SdkLifecycleState
import com.exponea.example.status.SdkStatusStore
import com.exponea.sdk.Exponea
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnonymizeDialogType {
    ANONYMIZED,
    STOP_INTEGRATION
}

data class AnonymizeDialogState(
    val type: AnonymizeDialogType,
    val title: String,
    val message: String,
    val confirmLabel: String,
    val dismissLabel: String? = null
)

data class AnonymizeUiState(
    val dialog: AnonymizeDialogState? = null
)

sealed interface AnonymizeUiEvent {
    data object NavigateToAuth : AnonymizeUiEvent
}

class AnonymizeViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnonymizeUiState())
    val state: StateFlow<AnonymizeUiState> = _state.asStateFlow()

    private val _events = Channel<AnonymizeUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onDialogCancel() {
        _state.update { it.copy(dialog = null) }
    }

    fun onDialogConfirm() {
        when (_state.value.dialog?.type) {
            AnonymizeDialogType.ANONYMIZED -> _state.update { it.copy(dialog = null) }
            AnonymizeDialogType.STOP_INTEGRATION -> executeStopIntegration(navigateToAuth = true)
            null -> Unit
        }
    }

    fun onDialogContinue() {
        if (_state.value.dialog?.type == AnonymizeDialogType.STOP_INTEGRATION) {
            executeStopIntegration(navigateToAuth = false)
        }
    }

    private fun executeStopIntegration(navigateToAuth: Boolean) {
        Exponea.stopIntegration {
            SdkSetupState.reset()
            SdkStatusStore.set(SdkLifecycleState.NOT_INITIALIZED)
            _state.update { it.copy(dialog = null) }
            if (navigateToAuth) {
                viewModelScope.launch { _events.send(AnonymizeUiEvent.NavigateToAuth) }
            }
        }
    }

    fun onAnonymizeClick() {
        Exponea.anonymize {
            SdkSetupState.reset()
            SdkStatusStore.set(SdkLifecycleState.ANONYMOUS)
            _state.update {
                it.copy(
                    dialog = AnonymizeDialogState(
                        type = AnonymizeDialogType.ANONYMIZED,
                        title = "Customer anonymized",
                        message = "Stored customer data cleared.",
                        confirmLabel = "OK"
                    )
                )
            }
        }
    }

    fun onStopIntegrationClick() {
        _state.update {
            it.copy(
                dialog = AnonymizeDialogState(
                    type = AnonymizeDialogType.STOP_INTEGRATION,
                    title = "Stop Integration",
                    message = "Do you want to go back to authentication or continue without integration?",
                    confirmLabel = "Back to Auth",
                    dismissLabel = "Continue"
                )
            )
        }
    }
}
