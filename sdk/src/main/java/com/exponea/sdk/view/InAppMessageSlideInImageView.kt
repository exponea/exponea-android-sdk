package com.exponea.sdk.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.exponea.sdk.R

internal class InAppMessageSlideInImageView(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    private val cornerRadius = context.resources.getDimension(R.dimen.exponea_sdk_in_app_message_slide_in_corner_radius)

    /**
     * Clip the image to a rounded rectangle.
     */
    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(getRoundPath())
        super.onDraw(canvas)
    }

    private fun getRoundPath(): Path {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        return Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
    }
}
