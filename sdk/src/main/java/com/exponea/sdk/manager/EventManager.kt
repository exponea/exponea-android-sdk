package com.exponea.sdk.manager

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue
import java.io.IOException

class EventManager(
        private val configuration: ExponeaConfiguration,
        private val eventRepository: EventRepository,
        private val apiManager: ExponeaApiManager
) {
    fun addEventToQueue(event: ExportedEventType) {
        eventRepository.add(event)
    }

    fun flushEvents() {
        val allEvents = eventRepository.all()
        val firstEvent = allEvents.firstOrNull()

        if (firstEvent != null) {
            Logger.i(this, "Flushing Event: ${firstEvent.id}")

        } else {
            Logger.i(this, "No events left to flush: ${allEvents.size}")
        }
    }

    private fun trySendingEvent(projectToken: String, event: ExportedEventType) {
        apiManager
                .postEvent(projectToken, event)
                .enqueue(
                        { _, response ->
                            onEventSentSuccess(response.isSuccessful, event)
                        },
                        { _, ioException ->
                            onEventSentError(ioException, event)
                        }
                )
    }

    private fun onEventSentSuccess(isSuccessful: Boolean, event: ExportedEventType) {
        Logger.d(this, "onEventSentSuccess: $isSuccessful -> ${event.id}")
        if (isSuccessful) {
            eventRepository.remove(event.id)
        } else {
            // Do nothing?
        }
    }

    private fun onEventSentError(exception: IOException, event: ExportedEventType) {
        Logger.e(
                this@EventManager,
                "Sending Event Failed (Event: ${event.id}) Sending back to queue",
                exception
        )
    }
}