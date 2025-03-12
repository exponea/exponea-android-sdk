package com.exponea.sdk.style

import androidx.annotation.ColorInt

internal data class ContainerStyle(
    @ColorInt val backgroundColor: Int,
    @ColorInt val backgroundOverlayColor: Int?,
    val buttonsAlignment: ButtonAlignment,
    val imagePosition: ImagePosition,
    val containerMargin: LayoutSpacing,
    val containerPadding: LayoutSpacing,
    val containerRadius: PlatformSize,
    val containerPosition: MessagePosition
)
