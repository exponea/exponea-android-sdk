package com.exponea.sdk.repository

import com.exponea.sdk.models.ExportedEventType

internal interface EventRepository {
    fun all(): List<ExportedEventType>
    fun count(): Int
    fun add(item: ExportedEventType)
    fun update(item: ExportedEventType)
    fun get(id: String): ExportedEventType?
    fun remove(id: String)
    fun clear()
}
