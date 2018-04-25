package com.exponea.sdk.manager

import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.Route
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger

class EventManagerImpl(
        private val configuration: ExponeaConfiguration,
        private val eventRepository: EventRepository
) : EventManager {
    override fun addEventToQueue(event: ExportedEventType) {
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
}