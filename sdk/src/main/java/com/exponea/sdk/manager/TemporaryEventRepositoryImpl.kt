package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.EventRepositoryImpl

/**
 * Writes changes into DB, but method 'all' returns only events stored from time of creation of this instance.
 */
internal class TemporaryEventRepositoryImpl(
    context: Context,
    preferences: ExponeaPreferencesImpl
) : EventRepositoryImpl(context, preferences) {

    private val runtimeDatabase = mutableMapOf<String, ExportedEvent>()

    override fun all(): List<ExportedEvent> {
        return runtimeDatabase.values.toList()
    }

    override fun add(item: ExportedEvent) {
        super.add(item)
        runtimeDatabase.put(item.id, item)
    }

    override fun update(item: ExportedEvent) {
        database.update(item)
        runtimeDatabase.put(item.id, item)
    }

    override fun remove(id: String) {
        database.remove(id)
        runtimeDatabase.remove(id)
    }

    override fun clear() {
        database.clear()
        runtimeDatabase.clear()
    }
}
