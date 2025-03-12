package com.exponea.sdk.style

import com.exponea.sdk.util.Logger

internal enum class MessagePosition(val value: String) {
    TOP("top"),
    BOTTOM("bottom");

    companion object {
        fun parse(from: String?): MessagePosition? {
            return MessagePosition.values().firstOrNull {
                it.value == from
            } ?: kotlin.run {
                Logger.e(this, "Unable to parse message position: $from")
                null
            }
        }
    }
}
