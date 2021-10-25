package com.exponea.sdk.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.exponea.sdk.models.ExportedEventType

@Dao
internal interface ExportedEventTypeDao {

    @Query("SELECT * FROM exported_event_type")
    fun all(): List<ExportedEventType>

    @Query("SELECT * FROM exported_event_type WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<ExportedEventType>

    @Query("SELECT COUNT(*) FROM exported_event_type")
    fun count(): Int

    @Query("SELECT * FROM exported_event_type WHERE id = :id LIMIT 1")
    fun get(id: String): ExportedEventType

    @Insert
    fun add(item: ExportedEventType)

    @Update
    fun update(item: ExportedEventType)

    @Query("DELETE FROM exported_event_type WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM exported_event_type")
    fun clear()
}
