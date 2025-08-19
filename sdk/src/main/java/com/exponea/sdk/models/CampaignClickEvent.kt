package com.exponea.sdk.models

import com.exponea.sdk.util.currentTimeSeconds

internal data class CampaignClickEvent(
    var url: String,
    var timestamp: Double,
    var properties: PlatformProperty
) {
    internal constructor(source: Event) : this (
            url = source.properties!!["url"]!! as String,
            timestamp = source.timestamp ?: (source.properties?.get("timestamp") as? Double) ?: currentTimeSeconds(),
            properties = PlatformProperty(PlatformProperty.ANDROID_PLATFORM)
    )
}
