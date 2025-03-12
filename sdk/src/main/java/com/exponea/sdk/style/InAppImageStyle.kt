package com.exponea.sdk.style

internal data class InAppImageStyle(
    val enabled: Boolean,
    val sizing: ImageSizing,
    val ratioWidth: Int,
    val ratioHeight: Int,
    val scale: ImageScaling,
    val margin: LayoutSpacing,
    val radius: PlatformSize
)
