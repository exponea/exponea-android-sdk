package com.exponea.sdk.style

import android.graphics.Color
import com.exponea.sdk.R
import com.exponea.sdk.models.ButtonUiPayload
import com.exponea.sdk.models.CloseButtonUiPayload
import com.exponea.sdk.models.ImageUiPayload
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessageUiPayload
import com.exponea.sdk.models.TextUiPayload
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.util.ConversionUtils

internal class InAppRichstylePayloadBuilder(
    val drawableCache: DrawableCache,
    val fontCache: FontCache
) {

    companion object {
        internal const val DEFAULT_RATIO_WIDTH = 4
        internal const val DEFAULT_RATIO_HEIGHT = 3
        internal val DEFAULT_CLOSE_BUTTON_MARGIN = LayoutSpacing.parse("20dp")!!
        internal val DEFAULT_CLOSE_BUTTON_PADDING = LayoutSpacing.parse("8dp")!!
        internal val DEFAULT_CLOSE_BUTTON_SIZE = PlatformSize.parse("32dp")!!
        internal val DEFAULT_CLOSE_BUTTON_BACKGROUND_COLOR = ConversionUtils.parseColor("#BDCEE3")!!
    }

    fun build(payload: InAppMessagePayload?): InAppMessageUiPayload? {
        if (payload == null) { return null }
        return InAppMessageUiPayload(
            image = buildImagePayload(payload),
            title = buildTitlePayload(payload),
            content = buildContentPayload(payload),
            closeButton = buildCloseButtonPayload(payload),
            buttons = buildButtonsPayload(payload),
            container = buildContainerPayload(payload)
        )
    }

    private fun buildContainerPayload(payload: InAppMessagePayload): ContainerStyle {
        val imagePosition = ImagePosition.parse(payload)
        var backgroundOverlayColor: Int? = null
        if (imagePosition == ImagePosition.OVERLAY && payload.isImageOverlayEnabled == true) {
            // overlay mode has to be activate to handle backgroundOverlayColor
            backgroundOverlayColor = ConversionUtils.parseColor(payload.backgroundOverlayColor)
        }
        return ContainerStyle(
            backgroundColor = ConversionUtils.parseColor(payload.backgroundColor) ?: Color.WHITE,
            buttonsAlignment = ButtonAlignment.parse(payload.buttonsAlignment) ?: ButtonAlignment.CENTER,
            imagePosition = imagePosition,
            backgroundOverlayColor = backgroundOverlayColor,
            containerMargin = LayoutSpacing.parse(payload.containerMargin)?.withForcedPxToDp() ?: LayoutSpacing.NONE,
            containerPadding = LayoutSpacing.parse(payload.containerPadding)?.withForcedPxToDp() ?: LayoutSpacing.NONE,
            containerRadius = PlatformSize.parse(payload.containerRadius)?.withForcedPxToDp() ?: PlatformSize.ZERO,
            containerPosition = MessagePosition.parse(payload.messagePosition) ?: MessagePosition.BOTTOM
        )
    }

    private fun buildButtonsPayload(payload: InAppMessagePayload): List<ButtonUiPayload> {
        return payload.buttons?.mapNotNull { from ->
            if (from.text.isNullOrEmpty()) {
                return@mapNotNull null
            }
            if (from.isEnabled != true) {
                return@mapNotNull null
            }
            ButtonUiPayload(
                text = from.text,
                style = InAppButtonStyle(
                    sizing = ButtonSizing.parse(from.sizing) ?: ButtonSizing.HUG_TEXT,
                    backgroundColor = ConversionUtils.parseColor(from.backgroundColor) ?: Color.TRANSPARENT,
                    cornerRadius = PlatformSize.parse(from.radius)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                    margin = LayoutSpacing.parse(from.margin)?.withForcedPxToDp() ?: LayoutSpacing.NONE,
                    customTypeface = from.fontUrl?.let { fontCache.getTypeface(it) },
                    textStyle = from.textStyle?.mapNotNull { TextStyle.parse(it) } ?: listOf(),
                    textSize = PlatformSize.parse(from.textSize)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                    lineHeight = PlatformSize.parse(from.lineHeight)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                    padding = LayoutSpacing.parse(from.padding)?.withForcedPxToDp() ?: LayoutSpacing.NONE,
                    textColor = ConversionUtils.parseColor(from.textColor) ?: Color.WHITE,
                    borderEnabled = from.isBorderEnabled ?: false,
                    borderWeight = PlatformSize.parse(from.borderWeight)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                    borderColor = ConversionUtils.parseColor(from.borderColor) ?: Color.TRANSPARENT,
                    textAlignment = TextAlignment.parse(from.textAlignment) ?: TextAlignment.CENTER,
                    enabled = from.isEnabled
                ),
                originPayload = from
            )
        } ?: listOf()
    }

    private fun buildCloseButtonPayload(payload: InAppMessagePayload): CloseButtonUiPayload {
        return CloseButtonUiPayload(
            icon = drawableCache.getDrawable(
                payload.closeButtonIconUrl
            ) ?: drawableCache.getDrawable(
                R.drawable.in_app_message_close_button
            )!!,
            style = InAppCloseButtonStyle(
                margin = LayoutSpacing.parse(payload.closeButtonMargin)?.withForcedPxToDp()
                    ?: DEFAULT_CLOSE_BUTTON_MARGIN,
                padding = DEFAULT_CLOSE_BUTTON_PADDING,
                size = DEFAULT_CLOSE_BUTTON_SIZE,
                backgroundColor = ConversionUtils.parseColor(payload.closeButtonBackgroundColor)
                    ?: DEFAULT_CLOSE_BUTTON_BACKGROUND_COLOR,
                iconColor = ConversionUtils.parseColor(payload.closeButtonIconColor) ?: Color.WHITE,
                enabled = payload.isCloseButtonEnabled ?: true
            )
        )
    }

    private fun buildContentPayload(payload: InAppMessagePayload): TextUiPayload {
        return TextUiPayload(
            value = payload.bodyText,
            style = InAppLabelStyle(
                enabled = payload.isBodyEnabled ?: false,
                textSize = PlatformSize.parse(payload.bodyTextSize)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                textAlignment = TextAlignment.parse(payload.bodyTextAlignment) ?: TextAlignment.LEFT,
                textStyle = payload.bodyTextStyle?.mapNotNull { TextStyle.parse(it) } ?: listOf(),
                textColor = ConversionUtils.parseColor(payload.bodyTextColor) ?: Color.BLACK,
                customTypeface = payload.bodyFontUrl?.let { fontCache.getTypeface(it) },
                lineHeight = PlatformSize.parse(payload.bodyLineHeight)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                padding = LayoutSpacing.parse(payload.bodyPadding)?.withForcedPxToDp() ?: LayoutSpacing.NONE
            )
        )
    }

    private fun buildTitlePayload(payload: InAppMessagePayload): TextUiPayload {
        return TextUiPayload(
            value = payload.title,
            style = InAppLabelStyle(
                enabled = payload.isTitleEnabled ?: false,
                textSize = PlatformSize.parse(payload.titleTextSize)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                textAlignment = TextAlignment.parse(payload.titleTextAlignment) ?: TextAlignment.LEFT,
                textStyle = payload.titleTextStyle?.mapNotNull { TextStyle.parse(it) } ?: listOf(),
                textColor = ConversionUtils.parseColor(payload.titleTextColor) ?: Color.BLACK,
                customTypeface = payload.titleFontUrl?.let { fontCache.getTypeface(it) },
                lineHeight = PlatformSize.parse(payload.titleLineHeight)?.withForcedPxToDp() ?: PlatformSize.ZERO,
                padding = LayoutSpacing.parse(payload.titlePadding)?.withForcedPxToDp() ?: LayoutSpacing.NONE
            )
        )
    }

    private fun buildImagePayload(payload: InAppMessagePayload): ImageUiPayload {
        return ImageUiPayload(
            source = drawableCache.getDrawable(payload.imageUrl),
            style = InAppImageStyle(
                enabled = payload.isImageEnabled ?: false,
                sizing = ImageSizing.parse(payload.imageSizing) ?: ImageSizing.AUTO_HEIGHT,
                ratioWidth = ConversionUtils.parseNumber(payload.imageRatioWidth)?.toInt()
                    ?: DEFAULT_RATIO_WIDTH,
                ratioHeight = ConversionUtils.parseNumber(payload.imageRatioHeight)?.toInt()
                    ?: DEFAULT_RATIO_HEIGHT,
                scale = ImageScaling.parse(payload.imageScale) ?: ImageScaling.COVER,
                margin = LayoutSpacing.parse(payload.imageMargin)?.withForcedPxToDp() ?: LayoutSpacing.NONE,
                radius = PlatformSize.parse(payload.imageRadius)?.withForcedPxToDp() ?: PlatformSize.ZERO
            )
        )
    }
}
