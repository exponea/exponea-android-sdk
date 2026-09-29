package com.exponea.example.ui.flushing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exponea.example.ui.common.UiMessage
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.util.Logger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FlushingTab {
    FLUSH_MODE,
    FLUSH_PERIOD,
    LOG_LEVEL
}

data class FlushDialogState(
    val title: String,
    val message: String? = null
)

data class FlushingUiState(
    val selectedTab: FlushingTab = FlushingTab.FLUSH_MODE,
    val isFlushing: Boolean = false,
    val flushDialog: FlushDialogState? = null,
    val flushMode: FlushMode = FlushMode.IMMEDIATE,
    val currentPeriod: String = "",
    val currentLogLevel: String = ""
) {
    val currentMode: String get() = flushMode.name

    val periodDisplay: String
        get() = if (flushMode == FlushMode.PERIOD) {
            currentPeriod.ifEmpty { "—" }
        } else {
            "— (requires PERIOD mode)"
        }

    val periodHint: String?
        get() = if (flushMode == FlushMode.PERIOD) {
            null
        } else {
            "Flush period requires PERIOD mode (current: ${flushMode.name}). Tap PERIOD first."
        }
}

class FlushingViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        FlushingUiState(
            flushMode = Exponea.flushMode,
            currentPeriod = formatPeriod(Exponea.flushPeriod),
            currentLogLevel = Exponea.loggerLevel.name
        )
    )
    val state: StateFlow<FlushingUiState> = _state.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun onTabSelected(tab: FlushingTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun onFlushDialogDismiss() {
        _state.update { it.copy(flushDialog = null) }
    }

    fun onGetFlushModeClick() {
        _state.update { it.copy(flushMode = Exponea.flushMode) }
    }

    fun onSetFlushModeClick(mode: FlushMode) {
        Exponea.flushMode = mode
        _state.update { it.copy(flushMode = mode) }
        sendMessage("Flush mode → $mode")
    }

    fun onGetFlushPeriodClick() {
        _state.update { it.copy(currentPeriod = formatPeriod(Exponea.flushPeriod)) }
    }

    fun onSetFlushPeriodClick(seconds: Long) {
        Exponea.flushPeriod = FlushPeriod(seconds, TimeUnit.SECONDS)
        _state.update { it.copy(currentPeriod = "$seconds seconds") }
        sendMessage("Flush period → $seconds seconds")
    }

    fun onFlushDataClick() {
        _state.update { it.copy(isFlushing = true) }
        Exponea.flushData { result ->
            _state.update {
                it.copy(
                    isFlushing = false,
                    flushDialog = if (result.isSuccess) {
                        FlushDialogState(title = "Flush successful")
                    } else {
                        FlushDialogState(
                            title = "Flush failed",
                            message = result.exceptionOrNull()?.localizedMessage
                        )
                    }
                )
            }
        }
    }

    fun onGetLogLevelClick() {
        _state.update { it.copy(currentLogLevel = Exponea.loggerLevel.name) }
    }

    fun onSetLogLevelClick(level: Logger.Level) {
        Exponea.loggerLevel = level
        _state.update { it.copy(currentLogLevel = level.name) }
        sendMessage("Log level → $level")
    }

    private fun sendMessage(text: String) {
        viewModelScope.launch { _messages.send(UiMessage(text)) }
    }

    private fun formatPeriod(period: FlushPeriod): String {
        val seconds = period.timeUnit.toSeconds(period.amount)
        return "$seconds seconds"
    }
}
