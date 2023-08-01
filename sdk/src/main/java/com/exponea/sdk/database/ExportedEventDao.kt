package com.exponea.sdk.database

import com.exponea.sdk.models.ExportedEvent

internal interface ExportedEventDao {

    fun all(): List<ExportedEvent>

    fun count(): Long

    fun get(id: String): ExportedEvent?

    fun add(item: ExportedEvent)

    fun update(item: ExportedEvent)

    fun delete(id: String)

    fun clear()
}
