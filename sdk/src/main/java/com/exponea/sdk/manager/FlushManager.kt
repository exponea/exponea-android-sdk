package com.exponea.sdk.manager

typealias FlushFinishedCallback = (Result<Unit>) -> Unit

internal interface FlushManager {

    val isRunning: Boolean
    /**
     * Starts flushing all events to Exponea
     */
    fun flushData(onFlushFinished: FlushFinishedCallback? = null)
}
