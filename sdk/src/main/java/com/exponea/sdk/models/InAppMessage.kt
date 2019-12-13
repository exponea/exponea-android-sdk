package com.exponea.sdk.models

import android.graphics.Color
import com.exponea.sdk.util.Logger
import com.google.gson.annotations.SerializedName

internal data class InAppMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("message_type")
    val messageType: String,
    @SerializedName("frequency")
    val frequency: String,
    @SerializedName("payload")
    val payload: InAppMessagePayload,
    @SerializedName("variant_id")
    val variantId: Int,
    @SerializedName("variant_name")
    val variantName: String,
    @SerializedName("trigger")
    val trigger: InAppMessageTrigger,
    @SerializedName("date_filter")
    val dateFilter: DateFilter
)

internal data class InAppMessageTrigger(
    @SerializedName("type")
    val type: String?,
    @SerializedName("event_type")
    val eventType: String?
)

internal data class InAppMessagePayload(
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("title_text_color")
    val titleTextColor: String,
    @SerializedName("title_text_size")
    val titleTextSize: String,
    @SerializedName("body_text")
    val bodyText: String,
    @SerializedName("body_text_color")
    val bodyTextColor: String,
    @SerializedName("body_text_size")
    val bodyTextSize: String,
    @SerializedName("button_text")
    val buttonText: String,
    @SerializedName("button_type")
    val buttonType: String,
    @SerializedName("button_link")
    val buttonLink: String,
    @SerializedName("button_text_color")
    val buttonTextColor: String,
    @SerializedName("button_background_color")
    val buttonBackgroundColor: String,
    @SerializedName("background_color")
    val backgroundColor: String,
    @SerializedName("close_button_color")
    val closeButtonColor: String
) {
    companion object {
        fun parseFontSize(fontSize: String, defaultValue: Float): Float {
            try {
                return fontSize.replace("px", "").toFloat()
            } catch (e: Throwable) {
                Logger.w(this, "Unable to parse in-app message font size $e")
                return defaultValue
            }
        }

        fun parseColor(color: String, defaultValue: Int): Int {
            try {
                return Color.parseColor(color)
            } catch (e: Throwable) {
                Logger.w(this, "Unable to parse in-app message color $e")
                return defaultValue
            }
        }
    }
}
