package com.exponea.example.models

object SdkSetupState {
    var isStreamConfig: Boolean = false
    var isCustomerIdentified: Boolean = false

    fun reset() {
        isStreamConfig = false
        isCustomerIdentified = false
    }
}
