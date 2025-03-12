package com.exponea.sdk.view.component

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.AppCompatImageButton
import com.exponea.sdk.style.InAppCloseButtonStyle
import com.exponea.sdk.style.LayoutSpacing
import com.exponea.sdk.style.PlatformSize

internal class InAppCloseButtonView : AppCompatImageButton {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initDefaults()
    }

    private fun initDefaults() {
        scaleType = ScaleType.CENTER_INSIDE
    }

    fun applyStyle(style: InAppCloseButtonStyle) {
        applyVisibility(style.enabled)
        applyLayoutParams(style.size, style.margin)
        applyBackground(style.backgroundColor, style.size)
        applyPadding(style.padding)
        applyIconColor(style.iconColor)
    }

    private fun applyVisibility(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun applyIconColor(iconColor: Int) {
        setColorFilter(iconColor)
    }

    private fun applyLayoutParams(size: PlatformSize, margin: LayoutSpacing) {
        val newLayoutParams = layoutParams as MarginLayoutParams
        newLayoutParams.width = size.toPx()
        newLayoutParams.height = size.toPx()
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
        size: PlatformSize
    ) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            this.cornerRadius = size.toPrecisePx() / 2
        }
    }

    private fun applyPadding(padding: LayoutSpacing) {
        setPadding(
            padding.left.toPx(),
            padding.top.toPx(),
            padding.right.toPx(),
            padding.bottom.toPx()
        )
    }
}
