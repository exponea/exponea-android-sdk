package com.exponea.sdk.util

import android.graphics.Color

internal class UiUtils {
    companion object {
        fun parseFontSize(fontSize: String?, defaultValue: Float): Float {
            if (fontSize == null) return defaultValue
            try {
                return fontSize.replace("px", "").toFloat()
            } catch (e: Throwable) {
                Logger.w(this, "Unable to parse in-app message font size $e")
                return defaultValue
            }
        }

        fun parseColor(color: String?, defaultValue: Int): Int {
            if (color == null) return defaultValue
            try {
                return Color.parseColor(color)
            } catch (e: Throwable) {
                Logger.w(this, "Unable to parse in-app message color $e")
                return defaultValue
            }
        }
    }
}
