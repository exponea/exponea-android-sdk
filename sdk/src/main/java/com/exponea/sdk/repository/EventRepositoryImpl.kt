package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.database.ExponeaDatabase.Companion.safeDatabaseOperation
import com.exponea.sdk.models.ExportedEvent

internal open class EventRepositoryImpl(
    val context: Context
) : EventRepository {

    override fun count() = safeDatabaseOperation(context) { it.count() }

    override fun all() = safeDatabaseOperation(context) { it.all() }

    override fun add(item: ExportedEvent) = safeDatabaseOperation(context) { it.add(item) }

    override fun update(item: ExportedEvent) = safeDatabaseOperation(context) { it.update(item) }

    override fun get(id: String) = safeDatabaseOperation(context) { it.get(id) }

    override fun remove(id: String) = safeDatabaseOperation(context) { it.remove(id) }

    override fun clear() = safeDatabaseOperation(context) { it.clear() }

    override fun onIntegrationStopped() {
        clear()
        ExponeaDatabase.closeDatabase()
    }
}
