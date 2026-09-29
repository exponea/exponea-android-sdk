package com.exponea.example.status

import com.exponea.example.model.SdkSetupState
import com.exponea.sdk.Exponea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SdkStatusStore {

    private val _state = MutableStateFlow(SdkLifecycleState.NOT_INITIALIZED)
    val state: StateFlow<SdkLifecycleState> = _state.asStateFlow()

    fun set(next: SdkLifecycleState) {
        if (_state.value != next) {
            _state.value = next
        }
    }

    fun syncFromSdk() {
        set(resolveFromSdk())
    }

    private fun resolveFromSdk(): SdkLifecycleState = when {
        !Exponea.isInitialized -> SdkLifecycleState.NOT_INITIALIZED
        SdkSetupState.isCustomerIdentified -> SdkLifecycleState.IDENTIFIED
        else -> SdkLifecycleState.ANONYMOUS
    }
}
