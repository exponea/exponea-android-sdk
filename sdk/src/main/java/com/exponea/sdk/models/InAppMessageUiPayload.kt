package com.exponea.sdk.models

import android.graphics.drawable.Drawable
import com.exponea.sdk.style.ContainerStyle
import com.exponea.sdk.style.InAppButtonStyle
import com.exponea.sdk.style.InAppCloseButtonStyle
import com.exponea.sdk.style.InAppImageStyle
import com.exponea.sdk.style.InAppLabelStyle

internal data class InAppMessageUiPayload(
    val image: ImageUiPayload,
    val title: TextUiPayload,
    val content: TextUiPayload,
    val closeButton: CloseButtonUiPayload,
    val buttons: List<ButtonUiPayload>,
    val container: ContainerStyle
)

internal data class CloseButtonUiPayload(
    val icon: Drawable,
    val style: InAppCloseButtonStyle
)

internal data class ButtonUiPayload(
    val text: String,
    val style: InAppButtonStyle,
    val originPayload: InAppMessagePayloadButton
)

internal data class TextUiPayload(
    val value: String?,
    val style: InAppLabelStyle
)

internal data class ImageUiPayload(
    val source: Drawable?,
    val style: InAppImageStyle
)
