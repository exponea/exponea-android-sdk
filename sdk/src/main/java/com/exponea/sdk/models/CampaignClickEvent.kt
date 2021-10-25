package com.exponea.sdk.models

internal data class CampaignClickEvent(
    var url: String,
    var age: Double,
    var properties: PlatformProperty
) {
    internal constructor(source: Event) : this (
            url = source.properties!!["url"]!! as String,
            age = source.properties!!["age"]!! as Double,
            properties = PlatformProperty(PlatformProperty.ANDROID_PLATFORM)
    )
}
