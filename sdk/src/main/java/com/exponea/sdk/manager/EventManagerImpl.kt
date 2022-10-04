package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.Route
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger

internal class EventManagerImpl(
    private val configuration: ExponeaConfiguration,
    private val eventRepository: EventRepository,
    private val customerIdsRepository: CustomerIdsRepository,
    private val flushManager: FlushManager,
    private val onEventCreated: (Event, EventType) -> Unit
) : EventManager {

    fun addEventToQueue(event: Event, eventType: EventType) {
        Logger.d(this, "addEventToQueue")

        val route = when (eventType) {
            EventType.TRACK_CUSTOMER -> Route.TRACK_CUSTOMERS
            EventType.PUSH_TOKEN -> Route.TRACK_CUSTOMERS
            EventType.CAMPAIGN_CLICK -> Route.TRACK_CAMPAIGN
            else -> Route.TRACK_EVENTS
        }

        var projects = arrayListOf(configuration.mainExponeaProject)
        projects.addAll(configuration.projectRouteMap[eventType] ?: arrayListOf())
        for (project in projects.distinct()) {
            val exportedEvent = ExportedEvent(
                    type = event.type,
                    timestamp = event.timestamp,
                    age = event.age,
                    customerIds = event.customerIds,
                    properties = event.properties,
                    projectId = project.projectToken,
                    route = route,
                    exponeaProject = project
            )
            Logger.d(this, "Added Event To Queue: ${exportedEvent.id}")
            eventRepository.add(exportedEvent)
        }

        // If flush mode is set to immediate, events should be send to Exponea APP immediatelly
        if (Exponea.flushMode == FlushMode.IMMEDIATE) {
            flushManager.flushData()
        }
    }

    override fun track(
        eventType: String?,
        timestamp: Double?,
        properties: HashMap<String, Any>,
        type: EventType
    ) {
        val trackedProperties: HashMap<String, Any> = hashMapOf()
        trackedProperties.putAll(configuration.defaultProperties)
        trackedProperties.putAll(properties)

        val event = Event(
            type = eventType,
            timestamp = timestamp,
            customerIds = customerIdsRepository.get().toHashMap(),
            properties = trackedProperties
        )

        addEventToQueue(event, type)
        onEventCreated(event, type)
    }
}
