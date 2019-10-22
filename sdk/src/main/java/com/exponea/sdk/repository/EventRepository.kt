package com.exponea.sdk.repository

import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExportedEventType

internal interface EventRepository {
    fun all(): ArrayList<DatabaseStorageObject<ExportedEventType>>
    fun add(item: DatabaseStorageObject<ExportedEventType>): Boolean
    fun update(item: DatabaseStorageObject<ExportedEventType>): Boolean
    fun get(id: String): DatabaseStorageObject<ExportedEventType>?
    fun remove(id: String): Boolean
    fun clear(): Boolean
}
