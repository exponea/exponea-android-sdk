package com.exponea.sdk.manager

interface FlushManager {
    var onFlushFinishListener: (() -> Unit)?
    var isRunning: Boolean
    /**
     * Starts flushing all events to Exponea
     */
    fun flushData()
}
