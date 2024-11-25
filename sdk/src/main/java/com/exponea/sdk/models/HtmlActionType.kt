package com.exponea.sdk.models

enum class HtmlActionType(val value: String) {
    BROWSER("browser"),
    DEEPLINK("deep-link"),
    CLOSE("close");
    companion object {
        fun find(value: String?) = HtmlActionType.values().find { it.value.equals(value, ignoreCase = true) }
    }
}
