package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

internal data class InAppMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
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

/**
 * This is temporary, will change in the future.
 * We should filter based on events and properties.
 * For now, we get objects e.g. {type:"not important" url: "URL"}.
 * Check that URL = eventName
 */
internal data class InAppMessageTrigger(
    @SerializedName("include_pages")
    val includePages: List<Map<String, String>>
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
)
