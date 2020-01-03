package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.Route
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger

internal class EventManagerImpl(
    private val context: Context,
    private val configuration: ExponeaConfiguration,
    private val eventRepository: EventRepository,
    private val customerIdsRepository: CustomerIdsRepository,
    private val inAppMessageManager: InAppMessageManager
) : EventManager {

    fun addEventToQueue(event: ExportedEventType, eventType: EventType) {
        Logger.d(this, "addEventToQueue")

        // Get our default token
        val defaultToken = configuration.projectToken
        // Load our token map
        var routeTokenMap = configuration.projectTokenRouteMap[eventType] ?: arrayListOf()
        // Add our default token to our token map
        routeTokenMap.add(defaultToken)
        // Remove all non unique ids
        routeTokenMap = routeTokenMap.distinct().toMutableList()

        val route = when (eventType) {
            EventType.TRACK_CUSTOMER -> Route.TRACK_CUSTOMERS
            EventType.PUSH_TOKEN -> Route.TRACK_CUSTOMERS
            EventType.CAMPAIGN_CLICK -> Route.TRACK_CAMPAIGN
            else -> Route.TRACK_EVENTS
        }

        for (projectId in routeTokenMap) {
            val databaseStorageObject = DatabaseStorageObject(
                    projectId = projectId,
                    item = event,
                    route = route
            )
            Logger.d(this, "Added Event To Queue: ${databaseStorageObject.id}")
            eventRepository.add(databaseStorageObject)
        }

        // If flush mode is set to immediate, events should be send to Exponea APP immediatelly
        if (Exponea.flushMode == FlushMode.IMMEDIATE) {
            Exponea.component.flushManager.flushData()
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

        val event = ExportedEventType(
            type = eventType,
            timestamp = timestamp,
            customerIds = customerIdsRepository.get().toHashMap(),
            properties = trackedProperties
        )

        addEventToQueue(event, type)

        if (eventType != null) {
            inAppMessageManager.showRandom(eventType, EventManagerInAppMessageTrackingDelegate(context, this))
        }
    }
}
