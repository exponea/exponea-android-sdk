package com.exponea.sdk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.exponea.sdk.models.ExportedEvent

@Database(entities = [ExportedEvent::class], version = 1)
@TypeConverters(Converters::class)
internal abstract class ExponeaDatabase : RoomDatabase() {

    abstract fun exportedEventDao(): ExportedEventDao

    fun all(): List<ExportedEvent> {
        return exportedEventDao().all()
    }
    fun count(): Int {
        return exportedEventDao().count()
    }
    fun add(item: ExportedEvent) {
        exportedEventDao().add(item)
    }

    fun update(item: ExportedEvent) {
        exportedEventDao().update(item)
    }

    fun get(id: String): ExportedEvent? {
        return exportedEventDao().get(id)
    }
    fun remove(id: String) {
        exportedEventDao().delete(id)
    }
    fun clear() {
        exportedEventDao().clear()
    }
}
