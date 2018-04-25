package com.exponea.sdk.manager

import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.Route
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue

class EventManager(
        private val configuration: ExponeaConfiguration,
        private val eventRepository: EventRepository,
        private val apiManager: ExponeaApiManager
) {
    fun addEventToQueue(event: ExportedEventType) {
        Logger.d(this, "addEventToQueue")

        // Get our default token
        val defaultToken = configuration.projectToken
        // Load our token map
        var routeTokenMap = configuration.projectTokenRouteMap[Route.TRACK_EVENTS] ?: arrayListOf()
        // Add our default token to our token map
        routeTokenMap.add(defaultToken)
        // Remove all non unique ids
        routeTokenMap = routeTokenMap.distinct().toMutableList()

        for (projectId in routeTokenMap) {
            val databaseStorageObject = DatabaseStorageObject(projectId = projectId, item = event)
            Logger.d(this, "Added Event To Queue: ${databaseStorageObject.id}")
            eventRepository.add(databaseStorageObject)
        }

    }

    fun flushEvents() {
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
        apiManager
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
                                    this@EventManager,
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
        flushEvents()
    }

    private fun onEventSentFailed(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        databaseObject.tries++

        if (databaseObject.tries >= configuration.maxTries) {
            eventRepository.remove(databaseObject.id)
        } else {
            eventRepository.update(databaseObject)
        }

        flushEvents()
    }
}