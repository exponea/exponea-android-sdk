package com.exponea.sdk.manager

import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DatabaseStorageObject
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

        val allEvents = eventRepository.all().sortedBy { it.item.timestamp }
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
        databaseObject: DatabaseStorageObject<ExportedEventType>,
        onFlushFinished: FlushFinishedCallback?
    ) {
        updateBeforeSend(databaseObject)
        routeSendingEvent(databaseObject)?.enqueue(
            handleResponse(databaseObject, onFlushFinished),
            handleFailure(databaseObject, onFlushFinished)
        )
    }

    private fun updateBeforeSend(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        when (databaseObject.route) {
            Route.TRACK_CAMPAIGN -> {
                // campaign event needs to recalculate 'age' property from 'timestamp' before posting
                databaseObject.item.properties?.let { properties ->
                    if (properties.containsKey("timestamp")) {
                        properties["age"] = currentTimeSeconds() - (properties["timestamp"] as Double)
                        properties.remove("timestamp")
                    }
                }
            }
            Route.TRACK_EVENTS -> {
                if (databaseObject.item.type != Constants.EventTypes.push) {
                    // do not use age for push notifications
                    // timestamp can be modified to preserve sent->delivered->clicked order
                    databaseObject.item.timestamp?.let { timestamp ->
                        databaseObject.item.age = currentTimeSeconds().minus(timestamp)
                        databaseObject.item.timestamp = null
                    }
                }
            }
            else -> { /* do nothing */ }
        }
    }

    private fun handleFailure(
        databaseObject: DatabaseStorageObject<ExportedEventType>,
        onFlushFinished: FlushFinishedCallback?
    ): (Call, IOException) -> Unit {
        return { _, ioException ->
            Logger.e(
                this@FlushManagerImpl,
                "Sending Event Failed ${databaseObject.id}",
                ioException
            )
            onEventSentFailed(databaseObject)
            // Once done continue and try to flush the rest of events
            flushDataInternal(onFlushFinished)
        }
    }

    private fun handleResponse(
        databaseObject: DatabaseStorageObject<ExportedEventType>,
        onFlushFinished: FlushFinishedCallback?
    ): (Call, Response) -> Unit {
        return { _, response ->
            val responseCode = response.code()
            Logger.d(this, "Response Code: $responseCode")
            when (response.code()) {
                in 200..299 -> onEventSentSuccess(databaseObject)
                in 500..599 -> {
                    databaseObject.shouldBeSkipped = true
                    eventRepository.update(databaseObject)
                }
                else -> onEventSentFailed(databaseObject)
            }
            // Once done continue and try to flush the rest of events
            flushDataInternal(onFlushFinished)
            response.close()
        }
    }

    private fun routeSendingEvent(databaseObject: DatabaseStorageObject<ExportedEventType>): Call? {
        // for older event in database without exponeaProject, fallback to current configuration data
        @Suppress("DEPRECATION")
        val exponeaProject = databaseObject.exponeaProject ?: ExponeaProject(
            configuration.baseURL,
            databaseObject.projectId,
            configuration.authorization
        )
        return exponeaService.let {
            when (databaseObject.route) {
                Route.TRACK_EVENTS -> it.postEvent(exponeaProject, databaseObject.item)
                Route.TRACK_CUSTOMERS -> it.postCustomer(exponeaProject, databaseObject.item)
                Route.TRACK_CAMPAIGN -> it.postCampaignClick(exponeaProject, databaseObject.item)
                else -> {
                    Logger.e(this, "Couldn't find properly route")
                    return null
                }
            }
        }
    }

    private fun onEventSentSuccess(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        Logger.d(this, "onEventSentSuccess: ${databaseObject.id}")
        if (databaseObject.route == Route.TRACK_CUSTOMERS) {
            customerIdentifiedHandler()
        }
        eventRepository.remove(databaseObject.id)
    }

    private fun onEventSentFailed(databaseObject: DatabaseStorageObject<ExportedEventType>) {
        Logger.d(this, "Event ${databaseObject.id} failed")
        databaseObject.tries++
        databaseObject.shouldBeSkipped = true
        if (databaseObject.tries >= configuration.maxTries) {
            eventRepository.remove(databaseObject.id)
        } else {
            eventRepository.update(databaseObject)
        }
    }
}
