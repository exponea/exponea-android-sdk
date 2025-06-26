package com.exponea.sdk.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.util.Logger

@Database(entities = [ExportedEvent::class], version = 2)
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

    companion object {
        @Volatile
        private var INSTANCE: ExponeaDatabase? = null

        fun getInstance(context: Context): ExponeaDatabase {
            if (INSTANCE == null || !INSTANCE!!.isOpen) {
                synchronized(this) {
                    if (INSTANCE == null || !INSTANCE!!.isOpen) {
                        INSTANCE = buildDatabase(context)
                    }
                }
            }
            return INSTANCE!!
        }

        private fun buildDatabase(context: Context): ExponeaDatabase {
            val databaseBuilder = Room.databaseBuilder(
                context,
                ExponeaDatabase::class.java,
                "ExponeaEventDatabase"
            )
            databaseBuilder.enableMultiInstanceInvalidation()
            databaseBuilder.allowMainThreadQueries()
            databaseBuilder.fallbackToDestructiveMigrationOnDowngrade()
            databaseMigrations().forEach { migration ->
                databaseBuilder.addMigrations(migration)
            }
            val database = databaseBuilder.build()
            try {
                database.count()
            } catch (e: Exception) {
                Logger.e(this, "Error occurred while init-opening database", e)
            }
            return database
        }

        private fun databaseMigrations(): List<Migration> {
            val migration1to2 = object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    try {
                        db.execSQL("ALTER TABLE exported_event ADD COLUMN sdk_event_type TEXT")
                    } catch (e: Exception) {
                        if (e.message?.contains("duplicate column name") == true) {
                            Logger.d(this, "Column `sdk_event_type` already exists, skipping migration")
                        } else {
                            throw e
                        }
                    }
                }
            }
            return listOf(migration1to2)
        }
    }
}
