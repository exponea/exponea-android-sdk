package com.exponea.example.ui.fetching

import androidx.lifecycle.ViewModel
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FetchingUiState(
    val resultText: String = "Result will appear here.",
    val isLoading: Boolean = false,
    val showRecommendationDialog: Boolean = false
)

class FetchingViewModel : ViewModel() {

    private val _state = MutableStateFlow(FetchingUiState())
    val state: StateFlow<FetchingUiState> = _state.asStateFlow()

    fun onFetchConsentsClick() {
        _state.update { it.copy(isLoading = true) }
        Exponea.getConsents(
            onSuccess = { result -> onFetchSuccess(result) },
            onFailure = { result -> onFetchFailed(result) }
        )
    }

    fun onFetchRecommendationsClick() {
        _state.update { it.copy(showRecommendationDialog = true) }
    }

    fun onRecommendationDialogDismiss() {
        _state.update { it.copy(showRecommendationDialog = false) }
    }

    fun onFetchRecommendation(options: CustomerRecommendationOptions) {
        _state.update { it.copy(isLoading = true, showRecommendationDialog = false) }
        Exponea.fetchRecommendation(
            recommendationOptions = options,
            onSuccess = { result -> onFetchSuccess(result) },
            onFailure = { result -> onFetchFailed(result) }
        )
    }

    fun onFetchSegmentsClick() {
        val exposingCategory = "discovery"
        Exponea.getSegments(exposingCategory = exposingCategory, force = false) { segments ->
            _state.update {
                it.copy(resultText = "Segments for $exposingCategory category:\n$segments")
            }
        }
    }

    fun onClearResultClick() {
        _state.update { it.copy(resultText = "Result will appear here.") }
    }

    private fun <T> onFetchSuccess(result: Result<ArrayList<T>>) {
        _state.update { it.copy(isLoading = false, resultText = result.toString()) }
    }

    private fun onFetchFailed(result: Result<FetchError>) {
        _state.update {
            it.copy(
                isLoading = false,
                resultText = "Message: ${result.results.message}\nJson: ${result.results.jsonBody}"
            )
        }
    }
}
