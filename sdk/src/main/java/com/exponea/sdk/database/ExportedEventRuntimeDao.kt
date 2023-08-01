package com.exponea.sdk.database

import com.exponea.sdk.models.ExportedEvent
import java.util.Collections

/**
 * This exists as Fallback for cases when local DB cannot be started up.
 * Using of this implementation is reported.
 */
internal class ExportedEventRuntimeDao : ExportedEventDao {

    companion object {
        val data: MutableMap<String, ExportedEvent> = Collections.synchronizedMap(mutableMapOf<String, ExportedEvent>())
    }

    override fun all(): List<ExportedEvent> {
        return mutableListOf<ExportedEvent>().apply {
            addAll(data.values)
        }
    }

    override fun count(): Long {
        return data.size.toLong()
    }

    override fun get(id: String): ExportedEvent? {
        return data[id]
    }

    override fun add(item: ExportedEvent) {
        data[item.id] = item
    }

    override fun update(item: ExportedEvent) {
        data[item.id] = item
    }

    override fun delete(id: String) {
        data.remove(id)
    }

    override fun clear() {
        data.clear()
    }
}
