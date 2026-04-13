package com.exponea.sdk.services

import com.exponea.sdk.models.ExponeaConfiguration

internal open class IntegrationConfigFactory(
    private var configuration: ExponeaConfiguration
) {

    val integrationConfig
        get() = configuration.integrationConfig

    fun reset(newConfiguration: ExponeaConfiguration) {
        configuration = newConfiguration
    }
}
