package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class InAppMessagePayload(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("body_text")
    val bodyText: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("title_text_color")
    val titleTextColor: String? = null,
    @SerializedName("title_text_size")
    val titleTextSize: String? = null,
    @SerializedName("body_text_color")
    val bodyTextColor: String? = null,
    @SerializedName("body_text_size")
    val bodyTextSize: String? = null,
    @SerializedName("background_color")
    val backgroundColor: String? = null,
    @SerializedName("container_margin")
    val containerMargin: String? = null,
    @SerializedName("container_padding")
    val containerPadding: String? = null,
    @SerializedName("container_corner_radius")
    val containerRadius: String? = null,
    @SerializedName("overlay_color")
    val backgroundOverlayColor: String? = null,
    @SerializedName("buttons_align")
    val buttonsAlignment: String? = null,
    @SerializedName("text_position")
    val textPosition: String? = null,
    @SerializedName("image_enabled")
    val isImageEnabled: Boolean? = null,
    @SerializedName("image_size")
    val imageSizing: String? = null,
    @SerializedName("image_margin")
    val imageMargin: String? = null,
    @SerializedName("image_corner_radius")
    val imageRadius: String? = null,
    @SerializedName("image_aspect_ratio_width")
    val imageRatioWidth: String? = null,
    @SerializedName("image_aspect_ratio_height")
    val imageRatioHeight: String? = null,
    @SerializedName("image_object_fit")
    val imageScale: String? = null,
    @SerializedName("image_overlay_enabled")
    val isImageOverlayEnabled: Boolean? = null,
    @SerializedName("text_over_image")
    val isTextOverImage: Boolean? = null,
    @SerializedName("title_enabled")
    val isTitleEnabled: Boolean? = null,
    @SerializedName("title_format")
    val titleTextStyle: List<String>? = null,
    @SerializedName("title_align")
    val titleTextAlignment: String? = null,
    @SerializedName("title_line_height")
    val titleLineHeight: String? = null,
    @SerializedName("title_padding")
    val titlePadding: String? = null,
    @SerializedName("title_font_url")
    val titleFontUrl: String? = null,
    @SerializedName("body_enabled")
    val isBodyEnabled: Boolean? = null,
    @SerializedName("body_format")
    val bodyTextStyle: List<String>? = null,
    @SerializedName("body_align")
    val bodyTextAlignment: String? = null,
    @SerializedName("body_line_height")
    val bodyLineHeight: String? = null,
    @SerializedName("body_padding")
    val bodyPadding: String? = null,
    @SerializedName("body_font_url")
    val bodyFontUrl: String? = null,
    @SerializedName("close_button_enabled")
    val isCloseButtonEnabled: Boolean? = null,
    @SerializedName("close_button_margin")
    val closeButtonMargin: String? = null,
    @SerializedName("close_button_image_url")
    val closeButtonIconUrl: String? = null,
    @SerializedName("close_button_background_color")
    val closeButtonBackgroundColor: String? = null,
    @SerializedName("close_button_color")
    val closeButtonIconColor: String? = null,
    @SerializedName("message_position")
    val messagePosition: String? = null,
    @SerializedName("buttons")
    val buttons: List<InAppMessagePayloadButton>? = null
)

data class InAppMessagePayloadButton(
    @SerializedName("button_text")
    val text: String? = null,
    @SerializedName("button_type")
    val rawType: String? = null,
    @SerializedName("button_link")
    val link: String? = null,
    @SerializedName("button_text_color")
    val textColor: String? = null,
    @SerializedName("button_background_color")
    val backgroundColor: String? = null,
    @SerializedName("button_width")
    val sizing: String? = null,
    @SerializedName("button_corner_radius")
    val radius: String? = null,
    @SerializedName("button_margin")
    val margin: String? = null,
    @SerializedName("button_has_border")
    val isBorderEnabled: Boolean? = null,
    @SerializedName("button_font_size")
    val textSize: String? = null,
    @SerializedName("button_line_height")
    val lineHeight: String? = null,
    @SerializedName("button_padding")
    val padding: String? = null,
    @SerializedName("button_align")
    val textAlignment: String? = null,
    @SerializedName("button_border_color")
    val borderColor: String? = null,
    @SerializedName("button_border_width")
    val borderWeight: String? = null,
    @SerializedName("button_font_url")
    val fontUrl: String? = null,
    @SerializedName("button_font_format")
    val textStyle: List<String>? = null,
    @SerializedName("button_enabled")
    val isEnabled: Boolean? = null
) {
    val buttonType: InAppMessageButtonType
        get() {
            return InAppMessageButtonType.values().find { it.value == rawType } ?: InAppMessageButtonType.DEEPLINK
        }
}

enum class InAppMessageType(val value: String) {
    MODAL("modal"),
    ALERT("alert"),
    FULLSCREEN("fullscreen"),
    SLIDE_IN("slide_in"),
    FREEFORM("freeform")
}

enum class InAppMessageButtonType(val value: String) {
    CANCEL("cancel"),
    DEEPLINK("deep-link"),
    BROWSER("browser")
}
