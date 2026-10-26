package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.repository.EventRepositoryImpl
import java.util.Collections

/**
 * Writes changes into DB, but method 'all' returns only events stored from time of creation of this instance.
 */
internal class TemporaryEventRepositoryImpl(
    context: Context
) : EventRepositoryImpl(context) {

    // Insertion order decides between events with equal timestamps. Iterations must hold the map lock.
    private val runtimeDatabase = Collections.synchronizedMap(LinkedHashMap<String, ExportedEvent>())

    override fun all(): List<ExportedEvent> {
        return synchronized(runtimeDatabase) { runtimeDatabase.values.toList() }
    }

    override fun count(): Int {
        return runtimeDatabase.size
    }

    override fun nextToFlush(): ExportedEvent? {
        return synchronized(runtimeDatabase) {
            runtimeDatabase.values
                .filter { !it.shouldBeSkipped }
                .minWithOrNull(compareBy(nullsFirst()) { it.timestamp })
        }
    }

    override fun markSkipped(id: String) {
        super.markSkipped(id)
        synchronized(runtimeDatabase) {
            runtimeDatabase[id]?.let { runtimeDatabase[id] = it.copy(shouldBeSkipped = true) }
        }
    }

    override fun markFailed(id: String) {
        super.markFailed(id)
        synchronized(runtimeDatabase) {
            runtimeDatabase[id]?.let { runtimeDatabase[id] = it.copy(tries = it.tries + 1, shouldBeSkipped = true) }
        }
    }

    override fun resetSkipped() {
        // Only events of this instance are reset, other stored events keep their state
        val skipped = synchronized(runtimeDatabase) { runtimeDatabase.values.filter { it.shouldBeSkipped } }
        skipped.forEach { skippedEvent ->
            super.update(skippedEvent.copy(shouldBeSkipped = false))
            // An event removed in the meantime must not come back, state changes since the snapshot are kept
            synchronized(runtimeDatabase) {
                runtimeDatabase[skippedEvent.id]?.let { runtimeDatabase[it.id] = it.copy(shouldBeSkipped = false) }
            }
        }
    }

    override fun add(item: ExportedEvent) {
        super.add(item)
        runtimeDatabase[item.id] = item
    }

    override fun update(item: ExportedEvent) {
        super.update(item)
        runtimeDatabase[item.id] = item
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
