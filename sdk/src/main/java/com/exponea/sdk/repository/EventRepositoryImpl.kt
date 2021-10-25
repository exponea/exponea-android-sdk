package com.exponea.sdk.repository

import android.content.Context
import androidx.room.Room
import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.models.ExportedEventType

internal class EventRepositoryImpl(context: Context) : EventRepository {
    private val database = Room.databaseBuilder(
            context,
            ExponeaDatabase::class.java, "EventDatabase"
    ).enableMultiInstanceInvalidation()
    .allowMainThreadQueries().build()

    override fun count(): Int {
        return database.count()
    }

    override fun all(): List<ExportedEventType> {
        return database.all()
    }

    override fun add(item: ExportedEventType) {
        database.add(item)
    }

    override fun update(item: ExportedEventType) {
        database.update(item)
    }

    override fun get(id: String): ExportedEventType? {
        return database.get(id)
    }

    override fun remove(id: String) {
        database.remove(id)
    }

    override fun clear() {
        database.clear()
    }
}
