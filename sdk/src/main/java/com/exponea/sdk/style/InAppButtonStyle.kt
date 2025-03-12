package com.exponea.sdk.style

import android.graphics.Typeface
import androidx.annotation.ColorInt

internal data class InAppButtonStyle(
    val sizing: ButtonSizing,
    @ColorInt val backgroundColor: Int,
    val cornerRadius: PlatformSize,
    val margin: LayoutSpacing,
    val customTypeface: Typeface?,
    val textStyle: List<TextStyle>,
    val textSize: PlatformSize,
    val lineHeight: PlatformSize,
    val padding: LayoutSpacing,
    @ColorInt val textColor: Int,
    val borderEnabled: Boolean,
    val borderWeight: PlatformSize,
    @ColorInt val borderColor: Int,
    val textAlignment: TextAlignment,
    val enabled: Boolean
)
