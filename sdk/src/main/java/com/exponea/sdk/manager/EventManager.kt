package com.exponea.sdk.manager

import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.repository.EventRepository

class EventManager(
        private val eventRepository: EventRepository,
        private val apiManager: ExponeaApiManager
) {
    fun addEventToQueue(event: ExportedEventType) {
        eventRepository.add(event)
    }
}