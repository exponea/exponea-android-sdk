package com.exponea.example.model

import com.exponea.example.config.AuthFormState
import com.exponea.example.config.IntegrationMode
import com.exponea.sdk.models.ExponeaConfiguration

data class SdkInitSnapshot(
    val tokenTrackFrequency: String,
    val regenerateDeviceIdOnAnonymize: Boolean,
    val applicationId: String
)

object SdkSetupState {
    var isStreamConfig: Boolean = false
    var isCustomerIdentified: Boolean = false
    var initSnapshot: SdkInitSnapshot? = null

    fun storeInitConfig(
        form: AuthFormState,
        configuration: ExponeaConfiguration
    ) {
        isStreamConfig = form.mode == IntegrationMode.STREAM
        initSnapshot = SdkInitSnapshot(
            tokenTrackFrequency = formatTokenTrackFrequency(configuration.tokenTrackFrequency),
            regenerateDeviceIdOnAnonymize = configuration.regenerateDeviceIdOnAnonymize,
            applicationId = configuration.applicationId
        )
    }

    fun reset() {
        isStreamConfig = false
        isCustomerIdentified = false
        initSnapshot = null
    }

    private fun formatTokenTrackFrequency(frequency: ExponeaConfiguration.TokenFrequency): String =
        frequency.name.lowercase().replace('_', ' ')
}
