package com.exponea.sdk.repository

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.util.Logger

internal open class EventRepositoryImpl(
    context: Context
) : EventRepository {
    val database: ExponeaDatabase

    init {
        val databaseBuilder = Room.databaseBuilder(
            context,
            ExponeaDatabase::class.java,
            "ExponeaEventDatabase"
        )
        databaseBuilder.enableMultiInstanceInvalidation()
        databaseBuilder.allowMainThreadQueries()
        databaseMigrations().forEach { migration ->
            databaseBuilder.addMigrations(migration)
        }
        database = databaseBuilder.build()
    }

    private fun databaseMigrations(): List<Migration> {
        val migration1to2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exported_event ADD COLUMN sdk_event_type TEXT")
            }
        }
        return listOf(migration1to2)
    }

    companion object {
        internal const val KEY = "ExponeaPaperDbMigrationStatus"
    }

    override fun count(): Int {
        return database.count()
    }

    override fun all(): List<ExportedEvent> {
        return database.all()
    }

    override fun add(item: ExportedEvent) {
        database.add(item)
    }

    override fun update(item: ExportedEvent) {
        database.update(item)
    }

    override fun get(id: String): ExportedEvent? {
        return database.get(id)
    }

    override fun remove(id: String) {
        database.remove(id)
    }

    override fun clear() {
        database.clear()
    }

    override fun onIntegrationStopped() {
        if (!database.isOpen) {
            // after init, first query is required to open connection
            try {
                database.count()
            } catch (e: IllegalStateException) {
                Logger.e(this, "Unable to re-open database, clearing may be incomplete", e)
            }
        }
        if (database.isOpen) {
            try {
                database.clear()
            } catch (e: IllegalStateException) {
                Logger.e(this, "Unable to clear already cleared and closed database")
            }
            database.close()
        }
    }
}
