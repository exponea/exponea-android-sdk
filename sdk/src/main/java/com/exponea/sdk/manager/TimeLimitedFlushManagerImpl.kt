package com.exponea.sdk.manager

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.runWithTimeout

internal class TimeLimitedFlushManagerImpl(
    configuration: ExponeaConfiguration,
    eventRepository: EventRepository,
    exponeaService: ExponeaService,
    connectionManager: ConnectionManager,
    onEventUploaded: (ExportedEvent) -> Unit,
    val flushTimeLimit: Long
) : FlushManagerImpl(
    configuration,
    eventRepository,
    exponeaService,
    connectionManager,
    onEventUploaded
) {
    override fun flushData(onFlushFinished: FlushFinishedCallback?) {
        runWithTimeout(flushTimeLimit, {
            super.flushData(onFlushFinished)
        }, {
            Logger.w(this, "Flushing timeouted")
        })
    }
}
