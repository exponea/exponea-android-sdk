package com.exponea.sdk.style

import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.util.Logger

internal enum class ImagePosition {
    PRIMARY,
    SECONDARY,
    OVERLAY;

    companion object {
        fun parse(from: InAppMessagePayload): ImagePosition {
            if (from.isTextOverImage == true) {
                return OVERLAY
            }
            if (ImageSizing.parse(from.imageSizing) == ImageSizing.FULLSCREEN) {
                return OVERLAY
            }
            return when (from.textPosition?.lowercase()) {
                "top" -> SECONDARY
                "bottom" -> PRIMARY
                "left" -> SECONDARY
                "right" -> PRIMARY
                else -> {
                    Logger.e(this, "Unable to parse image position: ${from.textPosition}")
                    PRIMARY
                }
            }
        }
    }
}
