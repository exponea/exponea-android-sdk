package com.exponea.sdk.models

import android.graphics.Color
import com.exponea.sdk.util.Logger
import com.google.gson.annotations.SerializedName

internal data class InAppMessagePayload(
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("title_text_color")
    val titleTextColor: String? = null,
    @SerializedName("title_text_size")
    val titleTextSize: String? = null,
    @SerializedName("body_text")
    val bodyText: String? = null,
    @SerializedName("body_text_color")
    val bodyTextColor: String? = null,
    @SerializedName("body_text_size")
    val bodyTextSize: String? = null,
    @SerializedName("buttons")
    val buttons: List<InAppMessagePayloadButton>? = null,
    @SerializedName("background_color")
    val backgroundColor: String? = null,
    @SerializedName("close_button_color")
    val closeButtonColor: String? = null,
    @SerializedName("text_position")
    val rawTextPosition: String? = null,
    @SerializedName("text_over_image")
    val isTextOverImage: Boolean? = null,
    @SerializedName("message_position")
    val rawMessagePosition: String? = null

) {
    val textPosition: TextPosition
        get() = if (rawTextPosition == "top") TextPosition.TOP else TextPosition.BOTTOM

    val messagePosition: MessagePosition
        get() = if (rawMessagePosition == "bottom") MessagePosition.BOTTOM else MessagePosition.TOP

    companion object {
        fun parseFontSize(fontSize: String?, defaultValue: Float): Float {
            if (fontSize == null) return defaultValue
            try {
                return fontSize.replace("px", "").toFloat()
            } catch (e: Throwable) {
                Logger.w(this, "Unable to parse in-app message font size $e")
                return defaultValue
            }
        }

        fun parseColor(color: String?, defaultValue: Int): Int {
            if (color == null) return defaultValue
            try {
                return Color.parseColor(color)
            } catch (e: Throwable) {
                Logger.w(this, "Unable to parse in-app message color $e")
                return defaultValue
            }
        }
    }
}

data class InAppMessagePayloadButton(
    @SerializedName("button_type")
    val rawButtonType: String? = null,
    @SerializedName("button_text")
    val buttonText: String? = null,
    @SerializedName("button_link")
    val buttonLink: String? = null,
    @SerializedName("button_background_color")
    val buttonBackgroundColor: String? = null,
    @SerializedName("button_text_color")
    val buttonTextColor: String? = null
) {
    val buttonType: InAppMessageButtonType
        get() {
            return InAppMessageButtonType.values().find { it.value == rawButtonType } ?: InAppMessageButtonType.DEEPLINK
        }
}

enum class InAppMessageType(val value: String) {
    MODAL("modal"),
    ALERT("alert"),
    FULLSCREEN("fullscreen"),
    SLIDE_IN("slide_in")
}

enum class TextPosition {
    TOP, BOTTOM
}

enum class MessagePosition {
    TOP, BOTTOM
}

enum class InAppMessageButtonType(val value: String) {
    CANCEL("cancel"),
    DEEPLINK("deep-link")
}
