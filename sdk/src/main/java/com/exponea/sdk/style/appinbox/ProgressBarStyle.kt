package com.exponea.style

import android.content.res.ColorStateList
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ProgressBar
import com.exponea.sdk.util.ConversionUtils

data class ProgressBarStyle(
    var visible: Boolean? = null,
    var progressColor: String? = null,
    var backgroundColor: String? = null
) {
    fun applyTo(target: ProgressBar) {
        visible?.let {
            target.visibility = if (it) View.VISIBLE else View.GONE
        }
        ConversionUtils.parseColor(progressColor)?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                target.progressTintList = ColorStateList.valueOf(it)
            } else {
                val progressDrawable: Drawable = target.getProgressDrawable().mutate()
                @Suppress("DEPRECATION")
                progressDrawable.setColorFilter(it, SRC_IN)
                target.setProgressDrawable(progressDrawable)
            }
        }
        ConversionUtils.parseColor(backgroundColor)?.let {
            target.setBackgroundColor(it)
        }
    }
}
