package com.exponea.sdk.repository

import com.exponea.sdk.database.PaperExponeaDatabase
import com.exponea.sdk.models.ExportedEventType

class EventRepository {
    private val database = PaperExponeaDatabase<ExportedEventType>("EventDatabase")

    fun all(): ArrayList<ExportedEventType> {
        return database.all()
    }

    fun add(item: ExportedEventType): Boolean {
        return database.add(item)
    }

    fun update(item: ExportedEventType): Boolean {
        return database.update(item)
    }

    fun get(id: String): ExportedEventType {
        return database.get(id)
    }

    fun remove(id: String): Boolean {
        return database.remove(id)
    }

    fun clear(): Boolean {
        return database.clear()
    }
}