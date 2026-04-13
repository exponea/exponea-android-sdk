package com.exponea.sdk.models

/**
 * [ExponeaConfiguration] overrides.
 */
class ExponeaConfigurationOverrides(
    val integrationRouteMap: Map<EventType, List<ProjectConfig>>? = null,
    val inAppContentBlockPlaceholdersAutoLoad: List<String>? = null
)
