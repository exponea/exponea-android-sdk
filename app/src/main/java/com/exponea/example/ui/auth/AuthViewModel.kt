package com.exponea.example.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exponea.example.config.AuthFormState
import com.exponea.example.config.IntegrationMode
import com.exponea.example.config.SdkInitResult
import com.exponea.example.config.SdkInitializer
import com.exponea.example.model.SdkSetupState
import com.exponea.example.status.SdkLifecycleState
import com.exponea.example.status.SdkStatusStore
import com.exponea.example.ui.common.UiMessage
import com.exponea.sdk.Exponea
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val mode: IntegrationMode = IntegrationMode.PROJECT,
    val projectToken: String = "",
    val authorizationToken: String = "",
    val advancedAuthKey: String = "",
    val streamId: String = "",
    val jwtKeyId: String = "",
    val jwtSecret: String = "",
    val registeredId: String = "",
    val apiUrl: String = "",
    val applicationId: String = ""
)

sealed interface AuthUiEvent {
    data object InitSuccess : AuthUiEvent
}

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun onModeChange(mode: IntegrationMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun onProjectTokenChange(value: String) {
        _state.update { it.copy(projectToken = value) }
    }

    fun onAuthorizationChange(value: String) {
        _state.update { it.copy(authorizationToken = value) }
    }

    fun onAdvancedAuthKeyChange(value: String) {
        _state.update { it.copy(advancedAuthKey = value) }
    }

    fun onStreamIdChange(value: String) {
        _state.update { it.copy(streamId = value) }
    }

    fun onJwtKeyIdChange(value: String) {
        _state.update { it.copy(jwtKeyId = value) }
    }

    fun onJwtSecretChange(value: String) {
        _state.update { it.copy(jwtSecret = value) }
    }

    fun onRegisteredIdChange(value: String) {
        _state.update { it.copy(registeredId = value) }
    }

    fun onApiUrlChange(value: String) {
        _state.update { it.copy(apiUrl = value) }
    }

    fun onApplicationIdChange(value: String) {
        _state.update { it.copy(applicationId = value) }
    }

    fun onStartClick() {
        val form = _state.value.toForm()
        if (!validateForm(form)) return
        when (val result = SdkInitializer.init(form)) {
            is SdkInitResult.Success -> {
                viewModelScope.launch { _events.send(AuthUiEvent.InitSuccess) }
            }
            is SdkInitResult.Error -> {
                sendMessage(result.message ?: "SDK init failed", long = true)
            }
        }
    }

    fun onClearLocalDataClick() {
        Exponea.clearLocalCustomerData()
        SdkSetupState.reset()
        SdkStatusStore.set(
            if (Exponea.isInitialized) SdkLifecycleState.ANONYMOUS else SdkLifecycleState.NOT_INITIALIZED
        )
        sendMessage("Local data cleared.")
    }

    private fun AuthUiState.toForm(): AuthFormState = AuthFormState(
        mode = mode,
        projectToken = projectToken.trim(),
        authorization = authorizationToken.trim(),
        advancedAuthKey = advancedAuthKey.trim(),
        streamId = streamId.trim(),
        jwtKeyId = jwtKeyId.trim(),
        jwtSecret = jwtSecret.trim(),
        registeredId = registeredId.trim(),
        apiUrl = apiUrl.trim(),
        applicationId = applicationId.trim()
    )

    private fun validateForm(form: AuthFormState): Boolean {
        val urlValid = form.apiUrl.isNotEmpty() &&
            Patterns.WEB_URL.matcher(form.apiUrl).matches() &&
            (form.apiUrl.startsWith("https://") || form.apiUrl.startsWith("http://"))
        val modeValid = when (form.mode) {
            IntegrationMode.PROJECT ->
                form.projectToken.isNotEmpty() && form.authorization.isNotEmpty()
            IntegrationMode.STREAM -> form.streamId.isNotEmpty()
        }
        if (!modeValid || !urlValid) {
            sendMessage("Please fill all required fields.")
            return false
        }
        if (form.mode == IntegrationMode.STREAM &&
            form.jwtKeyId.isNotBlank() != form.jwtSecret.isNotBlank()
        ) {
            sendMessage("JWT Key ID and JWT Secret must both be provided or both empty.", long = true)
            return false
        }
        return true
    }

    private fun sendMessage(text: String, long: Boolean = false) {
        viewModelScope.launch { _messages.send(UiMessage(text, long)) }
    }
}
