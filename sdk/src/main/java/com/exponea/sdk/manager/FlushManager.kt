package com.exponea.sdk.manager

internal interface FlushManager {
    var onFlushFinishListener: (() -> Unit)?
    val isRunning: Boolean
    /**
     * Starts flushing all events to Exponea
     */
    fun flushData()
}
