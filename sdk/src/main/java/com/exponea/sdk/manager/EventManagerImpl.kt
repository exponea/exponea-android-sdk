package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.IntegrationConfiguration
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.Route
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.services.IntegrationConfigFactory
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.getId

internal open class EventManagerImpl(
    private val configuration: ExponeaConfiguration,
    private val eventRepository: EventRepository,
    private val customerIdsRepository: CustomerIdsRepository,
    private val flushManager: FlushManager,
    private val integrationConfigFactory: IntegrationConfigFactory,
    private val onEventCreated: (Event, EventType) -> Unit,
    private val deviceIdProvider: () -> String
) : EventManager {

    fun addEventToQueue(event: Event, eventType: EventType, trackingAllowed: Boolean) {
        Logger.d(this, "addEventToQueue")

        val route = when (eventType) {
            EventType.TRACK_CUSTOMER -> Route.TRACK_CUSTOMERS
            EventType.CAMPAIGN_CLICK -> Route.TRACK_CAMPAIGN
            else -> Route.TRACK_EVENTS
        }

        val integrations = arrayListOf(integrationConfigFactory.integrationConfig)
        // Integration route map is only applicable for ProjectConfig
        if (configuration.integrationConfig is ProjectConfig) {
            integrations.addAll(configuration.integrationRouteMap[eventType] ?: arrayListOf())
        }
        ensureOnBackgroundThread {
            for (integration in integrations.distinct()) {
                val exportedEvent = ExportedEvent(
                    type = event.type,
                    timestamp = event.timestamp,
                    customerIds = event.customerIds,
                    properties = event.properties,
                    integrationConfiguration = IntegrationConfiguration(
                        integration.getId(),
                        baseUrl = integration.baseUrl,
                        authorization = if (integration is ProjectConfig) integration.authorization else null,
                        type = when (integration) {
                            is ProjectConfig -> IntegrationConfigType.PROJECT
                            is StreamConfig -> IntegrationConfigType.STREAM
                        }
                    ),
                    route = route,
                    sdkEventType = eventType.name
                )
                if (trackingAllowed) {
                    Logger.d(this, "Added Event To Queue: ${exportedEvent.id}")
                    eventRepository.add(exportedEvent)
                } else {
                    Logger.d(this, "Event has not been added to Queue: ${exportedEvent.id}" +
                        "because real tracking is not allowed")
                }
            }

            // If flush mode is set to immediate, events should be send to Exponea APP immediately
            if (Exponea.flushMode == FlushMode.IMMEDIATE) {
                flushManager.flushData()
            }
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
        if (Exponea.isStopped) {
            Logger.e(
                this,
                "Event ${type.name}${eventType?.let { "($it)" } ?: ""} has not been tracked, SDK is stopping"
            )
            return
        }
        val trackedProperties: HashMap<String, Any> = hashMapOf()
        if (canUseDefaultProperties(type)) {
            trackedProperties.putAll(configuration.defaultProperties)
        }
        trackedProperties.putAll(properties)

        if (type != EventType.TRACK_CUSTOMER) {
            trackedProperties["application_id"] = configuration.applicationId
            trackedProperties["device_id"] = deviceIdProvider()
        }

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
        notifyEventCreated(event, type)
    }

    internal fun notifyEventCreated(event: Event, type: EventType) {
        onEventCreated(event, type)
    }

    private fun canUseDefaultProperties(type: EventType): Boolean {
        return when (type) {
            EventType.TRACK_CUSTOMER -> configuration.allowDefaultCustomerProperties
            EventType.PUSH_TOKEN -> false
            else -> true
        }
    }
}
