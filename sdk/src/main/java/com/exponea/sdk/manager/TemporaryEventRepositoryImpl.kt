package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.repository.EventRepositoryImpl

/**
 * Writes changes into DB, but method 'all' returns only events stored from time of creation of this instance.
 */
internal class TemporaryEventRepositoryImpl(
    context: Context
) : EventRepositoryImpl(context) {

    private val runtimeDatabase = mutableMapOf<String, ExportedEvent>()

    override fun all(): List<ExportedEvent> {
        return runtimeDatabase.values.toList()
    }

    override fun add(item: ExportedEvent) {
        super.add(item)
        runtimeDatabase.put(item.id, item)
    }

    override fun update(item: ExportedEvent) {
        super.update(item)
        runtimeDatabase.put(item.id, item)
    }

    override fun remove(id: String) {
        super.remove(id)
        runtimeDatabase.remove(id)
    }

    override fun clear() {
        super.clear()
        runtimeDatabase.clear()
    }
}
