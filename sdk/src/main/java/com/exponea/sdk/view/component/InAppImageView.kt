package com.exponea.sdk.view.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.exponea.sdk.style.ImageScaling
import com.exponea.sdk.style.ImageSizing
import com.exponea.sdk.style.InAppImageStyle
import com.exponea.sdk.style.LayoutSpacing
import com.exponea.sdk.style.PlatformSize

internal class InAppImageView : AppCompatImageView {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var sizingRatio = 0f
    private var cornerRadius = 0

    fun applyStyle(style: InAppImageStyle) {
        applySizing(style.sizing, style.ratioWidth, style.ratioHeight)
        if (style.sizing == ImageSizing.FULLSCREEN) {
            applyScaling(ImageScaling.COVER)
            applyCornerRadius(PlatformSize.ZERO)
        } else {
            applyScaling(style.scale)
            applyCornerRadius(style.radius)
        }
        applyMargin(style.margin)
    }

    private fun applyCornerRadius(radius: PlatformSize) {
        // just store radius, will be used while drawing
        cornerRadius = radius.toPx()
    }

    private fun applyMargin(margin: LayoutSpacing) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            margin.left.toPx(),
            margin.top.toPx(),
            margin.right.toPx(),
            margin.bottom.toPx()
        )
        layoutParams = params
    }

    private fun applySizing(sizing: ImageSizing, ratioWidth: Int, ratioHeight: Int) {
        adjustViewBounds = when (sizing) {
            ImageSizing.AUTO_HEIGHT -> true
            ImageSizing.FULLSCREEN -> true
            ImageSizing.ASPECT_RATIO -> false
        }
        sizingRatio = when (sizing) {
            ImageSizing.AUTO_HEIGHT -> 0f
            ImageSizing.FULLSCREEN -> 0f
            ImageSizing.ASPECT_RATIO -> ratioWidth.toFloat() / ratioHeight
        }
    }

    private fun applyScaling(scale: ImageScaling) {
        scaleType = when (scale) {
            ImageScaling.COVER -> ScaleType.CENTER_CROP
            ImageScaling.FILL -> ScaleType.FIT_XY
            ImageScaling.CONTAIN -> ScaleType.FIT_CENTER
            ImageScaling.NONE -> ScaleType.CENTER
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (sizingRatio > 0) {
            val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
            val heightByRatio = (measuredWidth / sizingRatio).toInt()
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightByRatio, MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (cornerRadius > 0) {
            canvas.clipPath(Path().apply {
                addRoundRect(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    cornerRadius.toFloat(),
                    cornerRadius.toFloat(),
                    Path.Direction.CW
                )
            })
        }
        super.onDraw(canvas)
    }
}
