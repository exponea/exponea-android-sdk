package com.exponea.sdk.repository

import com.exponea.sdk.models.ExportedEvent

internal interface EventRepository {
    fun all(): List<ExportedEvent>
    fun count(): Int
    fun add(item: ExportedEvent)
    fun update(item: ExportedEvent)
    fun get(id: String): ExportedEvent?
    fun remove(id: String)
    fun clear()
}
