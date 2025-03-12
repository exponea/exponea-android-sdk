package com.exponea.sdk.view.layout

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.exponea.sdk.R

internal class RelativeLayoutWithMaxWidth : RelativeLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, sAttrs: Int, sRes: Int) : super(context, attrs, sAttrs, sRes) {
        init(context, attrs, sAttrs, sRes)
    }

    private var maxWidth = -1

    private fun init(context: Context, attrs: AttributeSet?, sAttrs: Int, sRes: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RelativeLayoutWithMaxWidth, sAttrs, sRes)
        maxWidth = typedArray.getDimensionPixelSize(R.styleable.RelativeLayoutWithMaxWidth_android_maxWidth, -1)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (maxWidth >= 0 && measuredWidth > maxWidth) {
            setMeasuredDimension(maxWidth, measuredHeight)
        }
    }

    fun setMaxWidth(value: Int) {
        maxWidth = value
        requestLayout()
        invalidate()
    }

    fun getMaxWidth() = maxWidth
}
