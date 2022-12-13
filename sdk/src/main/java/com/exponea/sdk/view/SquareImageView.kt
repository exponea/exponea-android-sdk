package com.exponea.sdk.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class SquareImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {

    private var maxMeasuredWidth: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        maxMeasuredWidth = Math.max(maxMeasuredWidth, MeasureSpec.getSize(widthMeasureSpec))
        setMeasuredDimension(maxMeasuredWidth, maxMeasuredWidth)
    }
}
