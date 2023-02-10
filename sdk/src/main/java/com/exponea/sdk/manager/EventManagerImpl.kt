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
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.util.Logger

internal class EventManagerImpl(
    private val configuration: ExponeaConfiguration,
    private val eventRepository: EventRepository,
    private val customerIdsRepository: CustomerIdsRepository,
    private val flushManager: FlushManager,
    private val projectFactory: ExponeaProjectFactory,
    private val onEventCreated: (Event, EventType) -> Unit
) : EventManager {

    fun addEventToQueue(event: Event, eventType: EventType, trackingAllowed: Boolean) {
        Logger.d(this, "addEventToQueue")

        val route = when (eventType) {
            EventType.TRACK_CUSTOMER -> Route.TRACK_CUSTOMERS
            EventType.PUSH_TOKEN -> Route.TRACK_CUSTOMERS
            EventType.CAMPAIGN_CLICK -> Route.TRACK_CAMPAIGN
            else -> Route.TRACK_EVENTS
        }

        var projects = arrayListOf(projectFactory.mainExponeaProject)
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
            if (trackingAllowed) {
                Logger.d(this, "Added Event To Queue: ${exportedEvent.id}")
                eventRepository.add(exportedEvent)
            } else {
                Logger.d(this, "Event has not been added to Queue: ${exportedEvent.id}" +
                    "because real tracking is not allowed")
            }
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
        type: EventType,
        customerIds: Map<String, String?>?
    ) {
        processTrack(eventType, timestamp, properties, type, true, customerIds)
    }

    override fun processTrack(
        eventType: String?,
        timestamp: Double?,
        properties: HashMap<String, Any>,
        type: EventType,
        trackingAllowed: Boolean,
        customerIds: Map<String, String?>?
    ) {
        val trackedProperties: HashMap<String, Any> = hashMapOf()
        if (canUseDefaultProperties(type)) {
            trackedProperties.putAll(configuration.defaultProperties)
        }
        trackedProperties.putAll(properties)
        val customerIdsMap: HashMap<String, String?> = hashMapOf()
        if (customerIds.isNullOrEmpty()) {
            customerIdsMap.putAll(customerIdsRepository.get().toHashMap())
        } else {
            customerIdsMap.putAll(customerIds)
        }
        val event = Event(
            type = eventType,
            timestamp = timestamp,
            customerIds = customerIdsMap,
            properties = trackedProperties
        )
        addEventToQueue(event, type, trackingAllowed)
        onEventCreated(event, type)
    }

    private fun canUseDefaultProperties(type: EventType): Boolean {
        return configuration.allowDefaultCustomerProperties || EventType.TRACK_CUSTOMER != type
    }
}
