package com.exponea.sdk.manager

import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue

class FlushManagerImpl(
        private val configuration: ExponeaConfiguration,
        private val eventRepository: EventRepository,
        private val exponeaService: ExponeaService
) : FlushManager {
    override fun flush() {
        val allEvents = eventRepository.all()

        Logger.d(this, "flushEvents: Count ${allEvents.size}")

        val firstEvent = allEvents.firstOrNull()

        if (firstEvent != null) {
            Logger.i(this, "Flushing Event: ${firstEvent.id}")
            trySendingEvent(firstEvent)
        } else {
            Logger.i(this, "No events left to flush: ${allEvents.size}")
        }
    }

    private fun trySendingEvent(
            databaseObject: DatabaseStorageObject<ExportedEventType>
    ) {
        exponeaService
                .postEvent(databaseObject.projectId, databaseObject.item)
                .enqueue(
                        { _, response ->
                            Logger.d(this, "Response Code: ${response.code()}")
                            if (response.isSuccessful) {
                                onEventSentSuccess(databaseObject)
                            } else {
                                onEventSentFailed(databaseObject)
                            }
                        },
                        { _, ioException ->
                            Logger.e(
                                    this@FlushManagerImpl,
                                    "Sending Event Failed ${databaseObject.id}",
                                    ioException
                            )
                            ioException.printStackTrace()
                            onEventSentFailed(databaseObject)
                        }
                )
    }

    private fun onEventSentSuccess(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        Logger.d(this, "onEventSentSuccess: ${databaseObject.id}")

        eventRepository.remove(databaseObject.id)
        // Once done continue and try to flush the rest of events
        flush()
    }

    private fun onEventSentFailed(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        databaseObject.tries++

        if (databaseObject.tries >= configuration.maxTries) {
            eventRepository.remove(databaseObject.id)
        } else {
            eventRepository.update(databaseObject)
        }

        flush()
    }
}