package com.exponea.sdk.view.layout

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

internal class FrameLayoutWithoutOverflow : FrameLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initDefaults()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, sAttrs: Int, sRes: Int) : super(context, attrs, sAttrs, sRes) {
        initDefaults()
    }

    private fun initDefaults() {
        clipChildren = true
        clipToPadding = true
    }

    private var lastMeasuredParentWidth = 0
    private var lastMeasuredParentHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measuredHeightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (measuredWidthMode != MeasureSpec.EXACTLY || measuredHeightMode != MeasureSpec.EXACTLY) {
            // parent size is unknown, we need to wait for it
            setMeasuredDimension(lastMeasuredParentWidth, lastMeasuredParentHeight)
            return
        }
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        lastMeasuredParentWidth = measuredWidth
        lastMeasuredParentHeight = measuredHeight
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
    }
}
