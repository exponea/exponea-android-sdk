package com.exponea.style

import android.content.res.ColorStateList
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
            target.progressTintList = ColorStateList.valueOf(it)
        }
        ConversionUtils.parseColor(backgroundColor)?.let {
            target.setBackgroundColor(it)
        }
    }
}
