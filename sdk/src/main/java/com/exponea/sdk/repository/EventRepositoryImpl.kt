package com.exponea.sdk.repository

import android.content.Context
import androidx.room.Room
import com.exponea.sdk.Exponea
import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.database.PaperDatabase
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger

internal open class EventRepositoryImpl(
    context: Context,
    private val preferences: ExponeaPreferences
) : EventRepository {
    private val oldDatabase = PaperDatabase(context, "EventDatabase")
    val database = Room.databaseBuilder(
            context,
            ExponeaDatabase::class.java, "ExponeaEventDatabase"
    ).enableMultiInstanceInvalidation()
    .allowMainThreadQueries().build()

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

    override fun tryToMigrateFromPaper() {
        // try to migrate from paperDB to room only once
        val migrationDone = preferences.getBoolean(KEY, false)
        if (migrationDone) return
        try {
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
                        type = it.item.type,
                        timestamp = it.item.timestamp,
                        age = it.item.age,
                        customerIds = it.item.customerIds,
                        properties = it.item.properties
                    )
                    database.add(exportedEvent)
                    oldDatabase.remove(it.id)
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
        preferences.setBoolean(KEY, true)
        }
}
