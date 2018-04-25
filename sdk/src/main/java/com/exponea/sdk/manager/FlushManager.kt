package com.exponea.sdk.manager

interface FlushManager {
    /**
     * Starts flushing all events to Exponea
     */
    fun flush()
}