package com.exponea.example.config

import com.exponea.example.model.SdkSetupState

data class ConfigEntry(
    val label: String,
    val value: String
)

object SdkConfigSummary {

    fun entries(): List<ConfigEntry> {
        val snapshot = SdkSetupState.initSnapshot ?: return listOf(
            ConfigEntry("Configuration", "Not available")
        )
        return listOf(
            ConfigEntry("Token track frequency", snapshot.tokenTrackFrequency),
            ConfigEntry(
                "Regenerate device ID on anonymize",
                if (snapshot.regenerateDeviceIdOnAnonymize) "true" else "false"
            ),
            ConfigEntry("Application ID", snapshot.applicationId)
        )
    }
}
