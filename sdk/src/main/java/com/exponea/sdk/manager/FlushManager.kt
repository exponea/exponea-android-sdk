package com.exponea.sdk.manager

interface FlushManager {
    var onFlushFinishListener: (() -> Unit)?
    val isRunning: Boolean
    /**
     * Starts flushing all events to Exponea
     */
    fun flushData()
}
