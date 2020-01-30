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

    /**
     * Clip the image to a rounded rectangle.
     * Draw the rectangle higher than image so that only top is clipped
     */
    override fun onDraw(canvas: Canvas?) {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat() + cornerRadius)
        val path = Path()
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas?.clipPath(path)
        super.onDraw(canvas)
    }
}
