package com.exponea.sdk.style

import com.exponea.sdk.util.Logger

internal enum class ButtonSizing(val value: String) {
    HUG_TEXT("hug_text"),
    FILL("fill");

    companion object {
        fun parse(from: String?): ButtonSizing? {
            return ButtonSizing.values().firstOrNull {
                it.value == from
            } ?: kotlin.run {
                Logger.e(this, "Unable to parse button sizing: $from")
                null
            }
        }
    }
}
