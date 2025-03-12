package com.exponea.sdk.style

import android.graphics.Typeface
import androidx.annotation.ColorInt

internal data class InAppLabelStyle(
    val enabled: Boolean,
    val textSize: PlatformSize,
    val textAlignment: TextAlignment,
    val textStyle: List<TextStyle>,
    @ColorInt val textColor: Int,
    val customTypeface: Typeface?,
    val lineHeight: PlatformSize,
    val padding: LayoutSpacing
)
