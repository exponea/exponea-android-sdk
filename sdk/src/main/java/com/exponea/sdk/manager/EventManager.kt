package com.exponea.sdk.manager

import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger

class EventManager(
        private val eventRepository: EventRepository,
        private val apiManager: ExponeaApiManager
) {
    fun addEventToQueue(event: ExportedEventType) {
        Logger.d(this, "addEventToQueue: ${event.id}")
        eventRepository.add(event)
    }
}