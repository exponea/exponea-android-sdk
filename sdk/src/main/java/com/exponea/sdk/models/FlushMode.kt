package com.exponea.sdk.models

import java.util.concurrent.TimeUnit

/**
 * Enumerator used for controlling how the SDK will send events to the backend
 */
enum class FlushMode {
    /**
     * Periodic data flushing will be flushing data in your specified interval (in seconds)
     * it will be automatically changed to APP_CLOSE to flush remaining events when app is in the background
     */
    PERIOD, APP_CLOSE,

    /**
     * Manual flushing mode disables any automatic upload and it's your responsibility to flush data.
     */
    MANUAL,
    /**
     * Flushes all data immediately as it is received.
     */
    IMMEDIATE
}

class FlushPeriod(private val amount: Long, private val timeUnit: TimeUnit) {
    var timeInMillis: Long = 0
        get() {
            return timeUnit.toMillis(amount)
        }
}