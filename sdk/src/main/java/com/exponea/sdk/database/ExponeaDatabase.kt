package com.exponea.sdk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.exponea.sdk.models.ExportedEventType

@Database(entities = [ExportedEventType::class], version = 1)
@TypeConverters(Converters::class)
internal abstract class ExponeaDatabase : RoomDatabase() {

    abstract fun exportedEventTypeDao(): ExportedEventTypeDao

    fun all(): List<ExportedEventType> {
        return exportedEventTypeDao().all()
    }
    fun count(): Int {
        return exportedEventTypeDao().count()
    }
    fun add(item: ExportedEventType) {
        exportedEventTypeDao().add(item)
    }

    fun update(item: ExportedEventType) {
        exportedEventTypeDao().update(item)
    }

    fun get(id: String): ExportedEventType? {
        return exportedEventTypeDao().get(id)
    }
    fun remove(id: String) {
        exportedEventTypeDao().delete(id)
    }
    fun clear() {
        exportedEventTypeDao().clear()
    }
}
