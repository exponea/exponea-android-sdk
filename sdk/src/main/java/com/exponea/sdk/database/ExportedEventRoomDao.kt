package com.exponea.sdk.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.exponea.sdk.models.ExportedEventRoom

@Dao
internal interface ExportedEventRoomDao {

    @Query("SELECT * FROM exported_event")
    fun all(): List<ExportedEventRoom>

    @Query("SELECT * FROM exported_event WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<ExportedEventRoom>

    @Query("SELECT COUNT(*) FROM exported_event")
    fun count(): Int

    @Query("SELECT * FROM exported_event WHERE id = :id LIMIT 1")
    fun get(id: String): ExportedEventRoom

    @Insert
    fun add(item: ExportedEventRoom)

    @Update
    fun update(item: ExportedEventRoom)

    @Query("DELETE FROM exported_event WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM exported_event")
    fun clear()
}
