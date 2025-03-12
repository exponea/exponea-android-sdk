package com.exponea.sdk.style

import android.util.TypedValue
import com.exponea.sdk.util.ConversionUtils
import kotlin.math.roundToInt

internal data class PlatformSize(val unit: Int, val size: Float) {
    companion object {
        val ZERO: PlatformSize = PlatformSize(TypedValue.COMPLEX_UNIT_PX, 0f)

        fun parse(from: String?): PlatformSize? {
            if (from.isNullOrBlank()) return null
            return PlatformSize(
                ConversionUtils.sizeType(from),
                ConversionUtils.sizeValue(from)
            )
        }
    }
    fun asString(): String {
        return "$size${ConversionUtils.sizeTypeString(unit)}"
    }

    fun toPx(): Int {
        return toPrecisePx().roundToInt()
    }

    fun toPrecisePx(): Float {
        return ConversionUtils.toPx(this)
    }

    /**
     * Returns new instance from current with unit changed to 'dp' if current unit is 'px' without changing size value.
     * Other unit types are not changed at all.
     */
    fun withForcedPxToDp(): PlatformSize {
        return PlatformSize(
            unit = if (unit == TypedValue.COMPLEX_UNIT_PX) TypedValue.COMPLEX_UNIT_DIP else unit,
            size = size
        )
    }
}
