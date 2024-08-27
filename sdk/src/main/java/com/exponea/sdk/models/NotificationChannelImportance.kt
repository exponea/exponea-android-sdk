package com.exponea.sdk.models

internal enum class NotificationChannelImportance(val code: Int, val trackValue: String) {
    // Represents NotificationManager.IMPORTANCE_UNSPECIFIED
    UNSPECIFIED(-1000, "importance_unspecified"),
    // Represents NotificationManager.IMPORTANCE_NONE
    NONE(0, "importance_none"),
    // Represents NotificationManager.IMPORTANCE_MIN
    MIN(1, "importance_min"),
    // Represents NotificationManager.IMPORTANCE_LOW
    LOW(2, "importance_low"),
    // Represents NotificationManager.IMPORTANCE_DEFAULT
    DEFAULT(3, "importance_default"),
    // Represents NotificationManager.IMPORTANCE_HIGH
    HIGH(4, "importance_high"),
    // Represents NotificationManager.IMPORTANCE_MAX
    MAX(5, "importance_max"),
    // Notification channel was not registered yet or cannot be retrieved
    UNKNOWN(-1000, "importance_unknown"),
    // Notification channel is not supported by (old) Android system
    UNSUPPORTED(-1000, "importance_unsupported")
}
