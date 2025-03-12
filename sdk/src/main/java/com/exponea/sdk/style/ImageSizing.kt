package com.exponea.sdk.style

import com.exponea.sdk.util.Logger

internal enum class ImageSizing(val value: String) {
    AUTO_HEIGHT("auto"),
    ASPECT_RATIO("lock_aspect_ratio"),
    FULLSCREEN("full_screen");

    companion object {
        fun parse(from: String?): ImageSizing? {
            return ImageSizing.values().firstOrNull {
                it.value == from
            } ?: kotlin.run {
                Logger.e(this, "Unable to parse image sizing: $from")
                null
            }
        }
    }
}
