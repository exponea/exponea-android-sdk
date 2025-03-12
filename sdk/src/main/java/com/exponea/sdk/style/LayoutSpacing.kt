package com.exponea.sdk.style

import com.exponea.sdk.util.Logger

internal data class LayoutSpacing(
    val left: PlatformSize,
    val top: PlatformSize,
    val right: PlatformSize,
    val bottom: PlatformSize
) {
    // Changes TypedValue.
    fun withForcedPxToDp(): LayoutSpacing {
        return LayoutSpacing(
            left = left.withForcedPxToDp(),
            top = top.withForcedPxToDp(),
            right = right.withForcedPxToDp(),
            bottom = bottom.withForcedPxToDp()
        )
    }

    companion object {
        val NONE: LayoutSpacing = LayoutSpacing(
            PlatformSize.ZERO,
            PlatformSize.ZERO,
            PlatformSize.ZERO,
            PlatformSize.ZERO
        )

        fun parse(from: String?): LayoutSpacing? {
            if (from.isNullOrBlank()) return null
            val reject = {
                Logger.e(this, "Unable to parse layout spacing: $from")
                null
            }
            val values = from.trim().split(" ")
            // according to https://www.w3schools.com/cssref/pr_padding.php
            return when (values.size) {
                1 -> {
                    val allSize = PlatformSize.parse(values[0]) ?: return reject()
                    LayoutSpacing(allSize, allSize, allSize, allSize)
                }
                2 -> {
                    val topBottomSize = PlatformSize.parse(values[0]) ?: return reject()
                    val leftRightSize = PlatformSize.parse(values[1]) ?: return reject()
                    LayoutSpacing(leftRightSize, topBottomSize, leftRightSize, topBottomSize)
                }
                3 -> {
                    val topSize = PlatformSize.parse(values[0]) ?: return reject()
                    val leftRightSize = PlatformSize.parse(values[1]) ?: return reject()
                    val bottomSize = PlatformSize.parse(values[2]) ?: return reject()
                    LayoutSpacing(leftRightSize, topSize, leftRightSize, bottomSize)
                }
                4 -> {
                    val topSize = PlatformSize.parse(values[0]) ?: return reject()
                    val rightSize = PlatformSize.parse(values[1]) ?: return reject()
                    val bottomSize = PlatformSize.parse(values[2]) ?: return reject()
                    val leftSize = PlatformSize.parse(values[3]) ?: return reject()
                    LayoutSpacing(leftSize, topSize, rightSize, bottomSize)
                }
                else -> reject()
            }
        }
    }
}
