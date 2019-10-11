package com.exponea.sdk.manager

import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.UniqueIdentifierRepository

internal class AnonymizeManagerImpl(
    private val eventRepository: EventRepository,
    private val uniqueIdentifierRepository: UniqueIdentifierRepository,
    private val customerIdsRepository: CustomerIdsRepository,
    private val sessionManager: SessionManager
) : AnonymizeManager {

    override fun anonymize() {
        eventRepository.clear()
        uniqueIdentifierRepository.clear()
        customerIdsRepository.clear()
        sessionManager.reset()
    }

}