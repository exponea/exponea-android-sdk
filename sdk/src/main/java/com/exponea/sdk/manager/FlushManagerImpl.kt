package com.exponea.sdk.manager

import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.Route
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.enqueue
import java.io.IOException
import kotlin.Result
import okhttp3.Call
import okhttp3.Response

internal class FlushManagerImpl(
    private val configuration: ExponeaConfiguration,
    private val eventRepository: EventRepository,
    private val exponeaService: ExponeaService,
    private val connectionManager: ConnectionManager,
    private val customerIdentifiedHandler: () -> Unit
) : FlushManager {
    @Volatile override var isRunning: Boolean = false
        private set

    override fun flushData(onFlushFinished: FlushFinishedCallback?) {
        synchronized(this) {
            if (isRunning) {
                onFlushFinished?.invoke(Result.failure(Exception("Flushing already in progress")))
                return
            }
            isRunning = true
        }
        flushDataInternal(onFlushFinished)
    }

    private fun flushDataInternal(onFlushFinished: FlushFinishedCallback?) {
        if (!connectionManager.isConnectedToInternet()) {
            Logger.d(this, "Internet connection is not available, skipping flush")
            onFlushFinished?.invoke(Result.failure(Exception("Internet connection is not available.")))
            isRunning = false
            return
        }

        val allEvents = eventRepository.all().sortedBy { it.timestamp }
        Logger.d(this, "flushEvents: Count ${allEvents.size}")

        val firstEvent = allEvents.firstOrNull { !it.shouldBeSkipped }

        if (firstEvent != null) {
            Logger.i(this, "Flushing Event: ${firstEvent.id}")
            trySendingEvent(firstEvent, onFlushFinished)
        } else {
            Logger.i(this, "No events left to flush: ${allEvents.size}")
            eventRepository.all().forEach {
                it.shouldBeSkipped = false
                eventRepository.update(it)
            }
            isRunning = false
            if (allEvents.isEmpty()) {
                onFlushFinished?.invoke(Result.success(Unit))
            } else {
                onFlushFinished?.invoke(
                    Result.failure(Exception("Failed to upload ${allEvents.size} events."))
                )
            }
        }
    }

    private fun trySendingEvent(
        exportedEvent: ExportedEventType,
        onFlushFinished: FlushFinishedCallback?
    ) {
        updateBeforeSend(exportedEvent)
        routeSendingEvent(exportedEvent)?.enqueue(
            handleResponse(exportedEvent, onFlushFinished),
            handleFailure(exportedEvent, onFlushFinished)
        )
    }

    private fun updateBeforeSend(exportedEvent: ExportedEventType) {
        when (exportedEvent.route) {
            Route.TRACK_CAMPAIGN -> {
                // campaign event needs to recalculate 'age' property from 'timestamp' before posting
                exportedEvent.properties?.let { properties ->
                    if (properties.containsKey("timestamp")) {
                        properties["age"] = currentTimeSeconds() - (properties["timestamp"] as Double)
                        properties.remove("timestamp")
                    }
                }
            }
            Route.TRACK_EVENTS -> {
                if (exportedEvent.type != Constants.EventTypes.push) {
                    // do not use age for push notifications
                    // timestamp can be modified to preserve sent->delivered->clicked order
                    exportedEvent.timestamp?.let { timestamp ->
                        exportedEvent.age = currentTimeSeconds().minus(timestamp)
                        exportedEvent.timestamp = null
                    }
                }
            }
            else -> { /* do nothing */ }
        }
    }

    private fun handleFailure(
        exportedEvent: ExportedEventType,
        onFlushFinished: FlushFinishedCallback?
    ): (Call, IOException) -> Unit {
        return { _, ioException ->
            Logger.e(
                this@FlushManagerImpl,
                "Sending Event Failed ${exportedEvent.id}",
                ioException
            )
            onEventSentFailed(exportedEvent)
            // Once done continue and try to flush the rest of events
            flushDataInternal(onFlushFinished)
        }
    }

    private fun handleResponse(
        exportedEvent: ExportedEventType,
        onFlushFinished: FlushFinishedCallback?
    ): (Call, Response) -> Unit {
        return { _, response ->
            val responseCode = response.code()
            Logger.d(this, "Response Code: $responseCode")
            when (response.code()) {
                in 200..299 -> onEventSentSuccess(exportedEvent)
                in 500..599 -> {
                    exportedEvent.shouldBeSkipped = true
                    eventRepository.update(exportedEvent)
                }
                else -> onEventSentFailed(exportedEvent)
            }
            // Once done continue and try to flush the rest of events
            flushDataInternal(onFlushFinished)
            response.close()
        }
    }

    private fun routeSendingEvent(exportedEvent: ExportedEventType): Call? {
        // for older event in database without exponeaProject, fallback to current configuration data
        @Suppress("DEPRECATION")
        val exponeaProject = exportedEvent.exponeaProject ?: ExponeaProject(
            configuration.baseURL,
                exportedEvent.projectId,
            configuration.authorization
        )
        val simpleEvent = Event(
                type = exportedEvent.type,
                timestamp = exportedEvent.timestamp,
                age = exportedEvent.age,
                customerIds = exportedEvent.customerIds,
                properties = exportedEvent.properties
        )
        return exponeaService.let {
            when (exportedEvent.route) {
                Route.TRACK_EVENTS -> it.postEvent(exponeaProject, simpleEvent)
                Route.TRACK_CUSTOMERS -> it.postCustomer(exponeaProject, simpleEvent)
                Route.TRACK_CAMPAIGN -> it.postCampaignClick(exponeaProject, simpleEvent)
                else -> {
                    Logger.e(this, "Couldn't find properly route")
                    return null
                }
            }
        }
    }

    private fun onEventSentSuccess(exportedEvent: ExportedEventType) {
        Logger.d(this, "onEventSentSuccess: ${exportedEvent.id}")
        if (exportedEvent.route == Route.TRACK_CUSTOMERS) {
            customerIdentifiedHandler()
        }
        eventRepository.remove(exportedEvent.id)
    }

    private fun onEventSentFailed(exportedEvent: ExportedEventType) {
        Logger.d(this, "Event ${exportedEvent.id} failed")
        exportedEvent.tries++
        exportedEvent.shouldBeSkipped = true
        if (exportedEvent.tries >= configuration.maxTries) {
            eventRepository.remove(exportedEvent.id)
        } else {
            eventRepository.update(exportedEvent)
        }
    }
}
