package com.exponea.sdk.style

import com.exponea.sdk.util.Logger

internal enum class ImageScaling(val value: String) {
    /**
     * Image is sized to maintain its aspect ratio while filling the element's
     * entire content box. If the object's aspect ratio does not match the aspect ratio of its box,
     * then the object will be clipped to fit.
     *
     * Android => CENTER_CROP
     */
    COVER("cover"),
    /**
     * Image is sized to fill the element's content box.
     * The entire object will completely fill the box.
     * If the object's aspect ratio does not match the aspect ratio of its box,
     * then the object will be stretched to fit.
     *
     * Android => FIT_XY
     */
    FILL("fill"),
    /**
     * Image is scaled to maintain its aspect ratio while fitting within the element's
     * content box. The entire object is made to fill the box, while preserving its aspect ratio,
     * so the object will be "letterboxed" if its aspect ratio does not match
     * the aspect ratio of the box.
     *
     * Android => FIT_CENTER
     */
    CONTAIN("contain"),
    /**
     * Image is not resized.
     *
     * Android => CENTER
     */
    NONE("none");

    companion object {
        fun parse(from: String?): ImageScaling? {
            return ImageScaling.values().firstOrNull {
                it.value == from
            } ?: kotlin.run {
                Logger.e(this, "Unable to parse image scaling: $from")
                null
            }
        }
    }
}
