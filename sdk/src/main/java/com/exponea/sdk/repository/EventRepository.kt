package com.exponea.sdk.repository

import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.services.OnIntegrationStoppedCallback

internal interface EventRepository : OnIntegrationStoppedCallback {
    fun all(): List<ExportedEvent>
    fun count(): Int
    fun add(item: ExportedEvent)
    fun update(item: ExportedEvent)
    fun get(id: String): ExportedEvent?
    fun remove(id: String)
    fun clear()
    override fun onIntegrationStopped()
}
