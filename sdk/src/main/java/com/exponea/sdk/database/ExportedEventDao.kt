package com.exponea.sdk.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.exponea.sdk.models.ExportedEvent

@Dao
internal interface ExportedEventDao {

    @Query("SELECT * FROM exported_event")
    fun all(): List<ExportedEvent>

    @Query("SELECT * FROM exported_event WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<ExportedEvent>

    @Query("SELECT * FROM exported_event WHERE should_be_skipped = 0 ORDER BY timestamp, rowid LIMIT 1")
    fun nextToFlush(): ExportedEvent?

    @Query("UPDATE exported_event SET should_be_skipped = 1 WHERE id = :id")
    fun markSkipped(id: String)

    @Query("UPDATE exported_event SET tries = tries + 1, should_be_skipped = 1 WHERE id = :id")
    fun markFailed(id: String)

    @Query("UPDATE exported_event SET should_be_skipped = 0 WHERE should_be_skipped = 1")
    fun resetSkipped()

    @Query("SELECT COUNT(*) FROM exported_event")
    fun count(): Int

    @Query("SELECT * FROM exported_event WHERE id = :id LIMIT 1")
    fun get(id: String): ExportedEvent?

    @Insert
    fun add(item: ExportedEvent)

    @Update
    fun update(item: ExportedEvent)

    @Query("DELETE FROM exported_event WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM exported_event")
    fun clear()
}
