package com.exponea.sdk.repository

import android.content.Context
import androidx.room.Room
import com.exponea.sdk.Exponea
import com.exponea.sdk.database.ExponeaRoomDatabase
import com.exponea.sdk.database.ExportedEventDao
import com.exponea.sdk.database.ExportedEventRealmDao
import com.exponea.sdk.database.ExportedEventRuntimeDao
import com.exponea.sdk.database.PaperDatabase
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger

internal open class EventRepositoryImpl(
    context: Context,
    private val preferences: ExponeaPreferences
) : EventRepository {

    private val ancestorDatabase = PaperDatabase(context, "EventDatabase")
    private val oldDatabase = Room.databaseBuilder(
            context,
            ExponeaRoomDatabase::class.java, "ExponeaEventDatabase"
    ).run {
        this.enableMultiInstanceInvalidation()
        .allowMainThreadQueries()
        .build()
    }

    val exportedEventDao: ExportedEventDao = buildExportedEventDao()

    private fun buildExportedEventDao(): ExportedEventDao {
        if (Exponea.isTestEnvironment()) {
            return ExportedEventRuntimeDao()
        }
        try {
            return ExportedEventRealmDao()
        } catch (t: Throwable) {
            Logger.e(this, "Unable to build Event local database, using fallback", t)
            Exponea.telemetry?.reportCaughtException(t)
            return ExportedEventRuntimeDao()
        }
    }

    companion object {
        internal const val PAPER_DB_MIGRATION_FLAG = "ExponeaPaperDbMigrationStatus"
        internal const val ROOM_DB_MIGRATION_FLAG = "ExponeaRoomDbMigrationStatus"
    }

    override fun count(): Long {
        return exportedEventDao.count()
    }

    override fun all(): List<ExportedEvent> {
        return exportedEventDao.all()
    }

    override fun add(item: ExportedEvent) {
        exportedEventDao.add(item)
    }

    override fun update(item: ExportedEvent) {
        exportedEventDao.update(item)
    }

    override fun get(id: String): ExportedEvent? {
        return exportedEventDao.get(id)
    }

    override fun remove(id: String) {
        return exportedEventDao.delete(id)
    }

    override fun clear() {
        exportedEventDao.clear()
    }

    override fun tryToMigrate() {
        tryToMigrateFromPaperDB()
        tryToMigrateFromRoomDB()
    }

    private fun tryToMigrateFromRoomDB() {
        val roomDbMigrationDone = preferences.getBoolean(ROOM_DB_MIGRATION_FLAG, false)
        if (roomDbMigrationDone) return
        try {
            // MIGRATION from Room
            val oldDbCount = oldDatabase.count()
            // do not migrate if old database containing a lot of events
            // db should contain only tens of events, more like thousand is an anomaly
            if (oldDbCount != 0 && oldDbCount < 1000) {
                oldDatabase.all().forEach {
                    val exportedEvent = ExportedEvent(
                        id = it.id,
                        tries = it.tries,
                        projectId = it.projectId,
                        route = it.route,
                        shouldBeSkipped = it.shouldBeSkipped,
                        exponeaProject = it.exponeaProject,
                        type = it.type,
                        timestamp = it.timestamp,
                        age = it.age,
                        customerIds = it.customerIds,
                        properties = it.properties
                    )
                    add(exportedEvent)
                    oldDatabase.remove(it.id)
                }
            }
            if (oldDbCount >= 1000) {
                Exponea.telemetry?.reportLog(
                    this,
                    "Migration skipped, too many events in Room database ($oldDbCount)"
                )
            }
        } catch (e: Exception) {
            Logger.e(this, "Unable to migrate data from Room database.", e)
        } finally {
            oldDatabase.close()
        }
        preferences.setBoolean(ROOM_DB_MIGRATION_FLAG, true)
    }

    private fun tryToMigrateFromPaperDB() {
        val paperDbMigrationDone = preferences.getBoolean(PAPER_DB_MIGRATION_FLAG, false)
        if (paperDbMigrationDone) return
        try {
            // MIGRATE from PaperDB
            val oldDbCount = ancestorDatabase.count()
            // do not migrate if old database containing a lot of events
            // db should contain only tens of events, more like thousand is an anomaly
            if (oldDbCount != 0 && oldDbCount < 1000) {
                ancestorDatabase.all().forEach {
                    val exportedEvent = ExportedEvent(
                        id = it.id,
                        tries = it.tries,
                        projectId = it.projectId,
                        route = it.route,
                        shouldBeSkipped = it.shouldBeSkipped,
                        exponeaProject = it.exponeaProject,
                        type = it.item.type,
                        timestamp = it.item.timestamp,
                        age = it.item.age,
                        customerIds = it.item.customerIds,
                        properties = it.item.properties
                    )
                    add(exportedEvent)
                    ancestorDatabase.remove(it.id)
                }
            }
            if (oldDbCount >= 1000) {
                Exponea.telemetry?.reportLog(
                    this,
                    "Migration skipped, too many events in paper database ($oldDbCount)"
                )
            }
        } catch (ex: Exception) {
            Logger.e(this, "Unable to migrate data from PaperDB database.", ex)
        }
        preferences.setBoolean(PAPER_DB_MIGRATION_FLAG, true)
    }
}
