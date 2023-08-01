package com.exponea.sdk.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.exponea.sdk.models.ExportedEventRoom

@Database(entities = [ExportedEventRoom::class], version = 1)
@TypeConverters(Converters::class)
internal abstract class ExponeaRoomDatabase : RoomDatabase() {

    abstract fun exportedEventDao(): ExportedEventRoomDao

    fun all(): List<ExportedEventRoom> {
        return exportedEventDao().all()
    }
    fun count(): Int {
        return exportedEventDao().count()
    }
    fun add(item: ExportedEventRoom) {
        exportedEventDao().add(item)
    }

    fun update(item: ExportedEventRoom) {
        exportedEventDao().update(item)
    }

    fun get(id: String): ExportedEventRoom? {
        return exportedEventDao().get(id)
    }
    fun remove(id: String) {
        exportedEventDao().delete(id)
    }
    fun clear() {
        exportedEventDao().clear()
    }
}
