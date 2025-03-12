package com.exponea.sdk.style

import com.exponea.sdk.util.Logger

internal enum class TextAlignment(val value: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    companion object {
        fun parse(from: String?): TextAlignment? {
            return TextAlignment.values().firstOrNull {
                it.value == from
            } ?: kotlin.run {
                Logger.e(this, "Unable to parse Text alignment: $from")
                null
            }
        }
    }
}
