package com.exponea.sdk.view.component

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import com.exponea.sdk.style.InAppLabelStyle
import com.exponea.sdk.style.PlatformSize
import com.exponea.sdk.style.TextAlignment
import com.exponea.sdk.style.TextStyle
import com.exponea.sdk.util.ConversionUtils
import kotlin.math.max

internal class InAppLabelView : AppCompatTextView {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initDefaults()
    }

    private fun initDefaults() {
        maxLines = Int.MAX_VALUE
    }

    internal fun applyStyle(style: InAppLabelStyle) {
        applyTextAlignment(style.textAlignment)
        applyTextSize(style.textSize)
        applyTextTypeface(style.customTypeface, style.textStyle)
        applyTextColor(style.textColor)
        applyLineHeight(style)
        applyPadding(style)
    }

    private fun applyPadding(style: InAppLabelStyle) {
        val cssTextPadding = ConversionUtils.simulateCssPaddingForText(style.padding, style.textSize, style.lineHeight)
        setPadding(
            cssTextPadding.left.toPx(),
            cssTextPadding.top.toPx(),
            cssTextPadding.right.toPx(),
            cssTextPadding.bottom.toPx()
        )
    }

    private fun applyLineHeight(style: InAppLabelStyle) {
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

    private fun applyTextAlignment(alignment: TextAlignment) {
        textAlignment = when (alignment) {
            TextAlignment.LEFT -> TEXT_ALIGNMENT_TEXT_START
            TextAlignment.CENTER -> TEXT_ALIGNMENT_CENTER
            TextAlignment.RIGHT -> TEXT_ALIGNMENT_TEXT_END
        }
    }
}
