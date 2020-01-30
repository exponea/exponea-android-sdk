package com.exponea.sdk.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.exponea.sdk.R

internal class InAppMessageDialogImageView(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    private val cornerRadius = context.resources.getDimension(R.dimen.exponea_sdk_in_app_message_dialog_corner_radius)

    var isOnTop: Boolean = true
    var textOverImage: Boolean = false
    /**
     * Clip the image to a rounded rectangle.
     */
    override fun onDraw(canvas: Canvas?) {
        val path = Path()
        path.addRoundRect(getRect(), cornerRadius, cornerRadius, Path.Direction.CW)
        canvas?.clipPath(path)
        super.onDraw(canvas)
    }

    private fun getRect(): RectF {
        // Draw all 4 corners rounded
        if (textOverImage) return RectF(0f, 0f, width.toFloat(), height.toFloat())
        // Draw the rectangle higher than image so that only top/bottom is clipped
        return if (isOnTop) RectF(0f, 0f, width.toFloat(), height.toFloat() + cornerRadius)
            else RectF(0f, -cornerRadius, width.toFloat(), height.toFloat())
    }
}
