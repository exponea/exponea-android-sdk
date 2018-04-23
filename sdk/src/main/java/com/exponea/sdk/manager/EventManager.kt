package com.exponea.sdk.manager

import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.repository.EventRepository
import okhttp3.Request

class EventManager(
        private val eventRepository: EventRepository,
        private val networkManager: ExponeaApiManager
) {
    fun addEventToQueue(event: ExportedEventType) {
        eventRepository.add(event)
    }

    fun flushEvents() {

        val request = Request.Builder()
                .
    }
}