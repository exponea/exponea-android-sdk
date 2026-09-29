package com.exponea.example.ui.contentblocks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ContentBlocksUiState(
    val showCarousels: Boolean = false,
    val exampleTopHeight: String = "Height: 0px",
    val defaultCarouselStatus: String = "Carousel loading…"
)

class ContentBlocksViewModel : ViewModel() {

    private val _state = MutableStateFlow(ContentBlocksUiState())
    val state: StateFlow<ContentBlocksUiState> = _state.asStateFlow()

    fun onShowCarouselsChanged(show: Boolean) {
        _state.update { it.copy(showCarousels = show) }
    }

    fun onExampleTopHeightUpdated(height: Int) {
        _state.update { it.copy(exampleTopHeight = "Height: ${height}px") }
    }

    fun onCarouselStatusUpdated(status: String) {
        _state.update { it.copy(defaultCarouselStatus = status) }
    }
}
