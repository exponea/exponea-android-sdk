package com.exponea.sdk.manager

internal interface FlushManager {
    var onFlushFinishListener: (() -> Unit)?
    var isRunning: Boolean
    /**
     * Starts flushing all events to Exponea
     */
    fun flushData()
}