package com.exponea.sdk.repository

import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.services.OnIntegrationStoppedCallback

internal interface EventRepository : OnIntegrationStoppedCallback {
    fun all(): List<ExportedEvent>
    /**
     * Oldest event that has not been marked as skipped, or null when there is none.
     */
    fun nextToFlush(): ExportedEvent?

    /**
     * Excludes the event from [nextToFlush] without counting a failed try.
     */
    fun markSkipped(id: String)

    /**
     * Increments the try counter and skips the event, as [markSkipped] does.
     */
    fun markFailed(id: String)

    /**
     * Makes all skipped events eligible for [nextToFlush] again.
     */
    fun resetSkipped()
    fun count(): Int
    fun add(item: ExportedEvent)
    fun update(item: ExportedEvent)
    fun get(id: String): ExportedEvent?
    fun remove(id: String)
    fun clear()
    override fun onIntegrationStopped()
}
