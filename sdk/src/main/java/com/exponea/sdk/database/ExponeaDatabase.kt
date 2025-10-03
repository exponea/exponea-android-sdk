package com.exponea.sdk.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.util.Logger

@Database(
    entities = [ExportedEvent::class],
    version = 3,
    autoMigrations = [
        AutoMigration(
            from = 2,
            to = 3,
            spec = ExponeaDatabase.Migration2to3::class
        )
    ]
)
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

        fun getInstance(context: Context): ExponeaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        fun <T> safeDatabaseOperation(context: Context, operation: (ExponeaDatabase) -> T): T {
            synchronized(this) {
                return operation(getInstance(context))
            }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context,
            ExponeaDatabase::class.java,
            "ExponeaEventDatabase"
        ).apply {
            enableMultiInstanceInvalidation()
            allowMainThreadQueries()
            fallbackToDestructiveMigrationOnDowngrade()
            addMigrations(*databaseMigrations())
        }.build()

        private fun databaseMigrations(): Array<Migration> = arrayOf(migration1to2)
    }

    @DeleteColumn(tableName = "exported_event", columnName = "age")
    class Migration2to3 : AutoMigrationSpec
}
