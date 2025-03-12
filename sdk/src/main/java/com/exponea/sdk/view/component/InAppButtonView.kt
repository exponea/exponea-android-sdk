package com.exponea.sdk.view.component

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import com.exponea.sdk.style.ButtonSizing
import com.exponea.sdk.style.InAppButtonStyle
import com.exponea.sdk.style.LayoutSpacing
import com.exponea.sdk.style.PlatformSize
import com.exponea.sdk.style.TextAlignment
import com.exponea.sdk.style.TextStyle
import com.exponea.sdk.util.ConversionUtils
import kotlin.math.max

internal class InAppButtonView : AppCompatButton {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun applyStyle(style: InAppButtonStyle) {
        val borderWeight = if (style.borderEnabled) style.borderWeight else PlatformSize.ZERO
        applyLayoutParams(style.sizing, style.margin)
        applyBackground(
            style.backgroundColor,
            style.cornerRadius,
            borderWeight,
            style.borderColor
        )
        applyTextTypeface(style.customTypeface, style.textStyle)
        applyTextSize(style.textSize)
        applyLineHeight(style)
        applyPadding(style, borderWeight)
        applyTextColor(style.textColor)
        applyTextAlignment(style.textAlignment)
    }

    private fun applyTextAlignment(textAlignment: TextAlignment) {
        this.textAlignment = when (textAlignment) {
            TextAlignment.LEFT -> TEXT_ALIGNMENT_TEXT_START
            TextAlignment.CENTER -> TEXT_ALIGNMENT_CENTER
            TextAlignment.RIGHT -> TEXT_ALIGNMENT_TEXT_END
        }
    }

    private fun applyLayoutParams(sizing: ButtonSizing, margin: LayoutSpacing) {
        val width = when (sizing) {
            ButtonSizing.FILL -> ViewGroup.LayoutParams.MATCH_PARENT
            ButtonSizing.HUG_TEXT -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
        val newLayoutParams = ViewGroup.MarginLayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        newLayoutParams.setMargins(
            margin.left.toPx(),
            margin.top.toPx(),
            margin.right.toPx(),
            margin.bottom.toPx()
        )
        layoutParams = newLayoutParams
    }

    private fun applyBackground(
        backgroundColor: Int,
        cornerRadius: PlatformSize,
        borderWeight: PlatformSize,
        borderColor: Int
    ) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            this.cornerRadius = cornerRadius.toPrecisePx()
            this.setStroke(borderWeight.toPx(), borderColor)
        }
    }

    private fun applyLineHeight(style: InAppButtonStyle) {
        val lineHeight = if (style.lineHeight.toPx() < style.textSize.toPx()) {
            style.textSize
        } else {
            style.lineHeight
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setLineHeight(lineHeight.unit, lineHeight.size)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setLineHeight(lineHeight.toPx())
        } else {
            // from TextViewCompat, but has to be called after typeface-setter
            val fontHeight = paint.getFontMetricsInt(null)
            setLineSpacing(max(0, lineHeight.toPx() - fontHeight).toFloat(), 1f)
        }
    }

    private fun applyPadding(style: InAppButtonStyle, borderWeight: PlatformSize) {
        val cssTextPadding = ConversionUtils.simulateCssPaddingForText(style.padding, style.textSize, style.lineHeight)
        // Android has "inner" border, CSS has "outer" border
        setPadding(
            cssTextPadding.left.toPx() + borderWeight.toPx(),
            cssTextPadding.top.toPx() + borderWeight.toPx(),
            cssTextPadding.right.toPx() + borderWeight.toPx(),
            cssTextPadding.bottom.toPx() + borderWeight.toPx()
        )
    }

    private fun applyTextSize(textSize: PlatformSize) {
        setTextSize(textSize.unit, textSize.size)
    }

    private fun applyTextColor(@ColorInt textColor: Int) {
        setTextColor(textColor)
    }

    private fun applyTextTypeface(typeFace: Typeface?, textStyle: List<TextStyle>) {
        val isBold = textStyle.contains(TextStyle.BOLD)
        val isItalic = textStyle.contains(TextStyle.ITALIC)
        val style = if (isBold && isItalic) {
            Typeface.BOLD_ITALIC
        } else if (isBold) {
            Typeface.BOLD
        } else if (isItalic) {
            Typeface.ITALIC
        } else {
            Typeface.NORMAL
        }
        setTypeface(typeFace, style)
    }
}
