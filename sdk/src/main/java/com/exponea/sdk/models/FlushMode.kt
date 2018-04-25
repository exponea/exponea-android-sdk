package com.exponea.sdk.models

import java.util.concurrent.TimeUnit

/**
 * Enumerator used for controlling how the SDK will send events to the backend
 */
enum class FlushMode {
    PERIOD, APP_CLOSE, MANUAL
}

class FlushPeriod(private val amount: Long, private val timeUnit: TimeUnit) {
    var timeInMillis: Long = 0
        get() {
            return timeUnit.toMillis(amount)
        }
}