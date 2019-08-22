package com.exponea.sdk.models

data class PlatformProperty(
    var platform: String = ANDROID_PLATFORM
) {
    companion object {
        val ANDROID_PLATFORM = "Android"
    }
}
