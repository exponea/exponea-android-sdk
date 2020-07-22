package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.database.ExponeaDatabaseImpl
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExportedEventType

internal class EventRepositoryImpl(context: Context) : EventRepository {
    private val database = ExponeaDatabaseImpl<ExportedEventType>(context, "EventDatabase")

    override fun count(): Int {
        return database.count()
    }

    override fun all(): ArrayList<DatabaseStorageObject<ExportedEventType>> {
        return database.all()
    }

    override fun add(item: DatabaseStorageObject<ExportedEventType>): Boolean {
        return database.add(item)
    }

    override fun update(item: DatabaseStorageObject<ExportedEventType>): Boolean {
        return database.update(item)
    }

    override fun get(id: String): DatabaseStorageObject<ExportedEventType>? {
        return database.get(id)
    }

    override fun remove(id: String): Boolean {
        return database.remove(id)
    }

    override fun clear(): Boolean {
        return database.clear()
    }
}
