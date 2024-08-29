package com.exponea.sdk.models

/**
 * Each action is used to trigger a different event when clicking a notification body or button
 */
enum class ExponeaNotificationActionType(val value: String) {
    APP("app"),
    BROWSER("browser"),
    DEEPLINK("deeplink"),
    SELFCHECK("self-check");

    companion object {
        fun find(value: String?) = ExponeaNotificationActionType.values().find { it.value == value }
    }
}
