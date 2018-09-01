package com.exponea.sdk.manager

import com.exponea.sdk.repository.DeviceInitiatedRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.UniqueIdentifierRepository

class AnonymizeManagerImpl(
    private val eventRepository: EventRepository,
    private val uniqueIdentifierRepository: UniqueIdentifierRepository,
    private val sessionManager: SessionManager,
    private val deviceInitiatedRepository: DeviceInitiatedRepository
) : AnonymizeManager {
    override fun anonymize() {
        eventRepository.clear()
        uniqueIdentifierRepository.clear()
        deviceInitiatedRepository.set(false)
        sessionManager.reset()
    }
}