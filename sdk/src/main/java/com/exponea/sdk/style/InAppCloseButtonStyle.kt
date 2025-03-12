package com.exponea.sdk.style

import androidx.annotation.ColorInt

internal data class InAppCloseButtonStyle(
    val margin: LayoutSpacing,
    val padding: LayoutSpacing,
    val size: PlatformSize,
    @ColorInt val backgroundColor: Int,
    @ColorInt val iconColor: Int,
    val enabled: Boolean
)
