package com.exponea.sdk.manager

import androidx.annotation.WorkerThread
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.Route
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.enqueue
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.logOnException
import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Call
import okhttp3.Response

internal open class FlushManagerImpl(
    private val configuration: ExponeaConfiguration,
    private val eventRepository: EventRepository,
    private val exponeaService: ExponeaService,
    private val connectionManager: ConnectionManager,
    private val customerIdsRepository: CustomerIdsRepository,
    private val onEventUploaded: (ExportedEvent) -> Unit
) : FlushManager {

    internal val isRunningAtomic = AtomicBoolean(false)
    override val isRunning: Boolean
        get() = isRunningAtomic.get()

    private val pendingCallbacks = Collections.synchronizedList(mutableListOf<FlushFinishedCallback>())

    internal fun tryStartFlushProcess() = isRunningAtomic.compareAndSet(false, true)

    internal fun endsFlushProcess() {
        if (!isRunningAtomic.compareAndSet(true, false)) {
            Logger.e(this, "Flushing process ends prematurely")
            isRunningAtomic.set(false)
        }
    }

    private fun drainPendingCallbacks(result: Result<Unit>) {
        val callbacks = synchronized(pendingCallbacks as Any) {
            val copy = pendingCallbacks.toList()
            pendingCallbacks.clear()
            copy
        }
        callbacks.forEach { runCatching { it(result) }.logOnException() }
    }

    override fun flushData(onFlushFinished: FlushFinishedCallback?) {
        if (Exponea.isStopped) {
            Logger.w(this, "Flushing has been denied, SDK is stopping")
            onFlushFinished?.invoke(Result.failure(Exception("Flushing denied, SDK is stopping")))
            drainPendingCallbacks(Result.failure(Exception("Flushing denied, SDK is stopping")))
            return
        }
        if (tryStartFlushProcess()) {
            flushDataInternal(onFlushFinished)
        } else {
            if (onFlushFinished != null) {
                pendingCallbacks.add(onFlushFinished)
            }
        }
    }

    private fun flushDataInternal(onFlushFinished: FlushFinishedCallback?) {
        if (Exponea.isStopped) {
            onFlushDenialForStoppedSdk(onFlushFinished)
            return
        }
        if (!connectionManager.isConnectedToInternet()) {
            Logger.d(this, "Internet connection is not available, skipping flush")
            finishFlush(onFlushFinished, Result.failure(Exception("Internet connection is not available.")))
            return
        }
        ensureOnBackgroundThread {
            runCatching {
                sendNextEvent(onFlushFinished)
            }.onFailure {
                Logger.e(this, "Flushing failed unexpectedly", it)
                finishFlush(onFlushFinished, Result.failure(Exception("Flushing failed unexpectedly.", it)))
            }
        }
    }

    private fun finishFlush(onFlushFinished: FlushFinishedCallback?, result: Result<Unit>) {
        endsFlushProcess()
        runCatching {
            onFlushFinished?.invoke(result)
            drainPendingCallbacks(result)
            return@runCatching
        }.logOnException()
    }

    private fun onFlushDenialForStoppedSdk(onFlushFinished: ((Result<Unit>) -> Unit)?) {
        Logger.w(this, "Flushing has been denied, SDK is stopping")
        finishFlush(onFlushFinished, Result.failure(Exception("Flushing denied, SDK is stopping")))
    }

    /**
     * Sends the oldest pending event, the response handler continues with the next one.
     * Events that can never be sent are removed and the next one is tried right away.
     * Events that fail to be prepared count as a failed try, so they cannot block the queue.
     */
    private fun sendNextEvent(onFlushFinished: FlushFinishedCallback?) {
        while (true) {
            val exportedEvent = eventRepository.nextToFlush()
            if (exportedEvent == null) {
                onNoEventToFlush(onFlushFinished)
                return
            }
            Logger.d(this, "Flushing Event: ${exportedEvent.id}")
            val call = try {
                routeSendingEvent(exportedEvent)
            } catch (e: Exception) {
                Logger.e(this, "Preparing Event ${exportedEvent.id} failed", e)
                onEventSentFailed(exportedEvent)
                continue
            }
            if (call == null) {
                eventRepository.remove(exportedEvent.id)
                continue
            }
            call.enqueue(
                handleResponse(exportedEvent, onFlushFinished),
                handleFailure(exportedEvent, onFlushFinished)
            )
            return
        }
    }

    private fun onNoEventToFlush(onFlushFinished: FlushFinishedCallback?) {
        // Skipped events were only skipped for this run, they are eligible for the next one
        eventRepository.resetSkipped()
        val remaining = eventRepository.count()
        val result = if (remaining == 0) {
            Logger.i(this, "Flush finished, queue is empty")
            Result.success(Unit)
        } else {
            Logger.w(
                this,
                "Flush finished, $remaining events could not be uploaded and are kept for next flush"
            )
            Result.failure(Exception("Failed to upload $remaining events."))
        }
        finishFlush(onFlushFinished, result)
    }

    private fun handleFailure(
        exportedEvent: ExportedEvent,
        onFlushFinished: FlushFinishedCallback?
    ): (Call, IOException) -> Unit {
        return { _, ioException ->
            if (Exponea.isStopped) {
                onFlushDenialForStoppedSdk(onFlushFinished)
            } else {
                ensureOnBackgroundThread {
                    Logger.e(
                        this@FlushManagerImpl,
                        "Sending Event Failed ${exportedEvent.id}",
                        ioException
                    )
                    continueFlushAfter(onFlushFinished) { onEventSentFailed(exportedEvent) }
                }
            }
        }
    }

    /**
     * Records the outcome of a request and continues with the rest of events. When recording fails the run
     * ends, otherwise the flush would stay marked as running and block all following runs.
     */
    private fun continueFlushAfter(onFlushFinished: FlushFinishedCallback?, recordOutcome: () -> Unit) {
        val failure = runCatching(recordOutcome).exceptionOrNull()
        if (failure != null) {
            Logger.e(this, "Flushing failed unexpectedly", failure)
            finishFlush(onFlushFinished, Result.failure(Exception("Flushing failed unexpectedly.", failure)))
            return
        }
        flushDataInternal(onFlushFinished)
    }

    private fun handleResponse(
        exportedEvent: ExportedEvent,
        onFlushFinished: FlushFinishedCallback?
    ): (Call, Response) -> Unit {
        return { _, response ->
            if (Exponea.isStopped) {
                onFlushDenialForStoppedSdk(onFlushFinished)
                response.close()
            } else {
                ensureOnBackgroundThread {
                    continueFlushAfter(onFlushFinished) {
                        response.use {
                            Logger.d(this, "Response Code: ${it.code}")
                            when (it.code) {
                                in 200..299 -> onEventSentSuccess(exportedEvent)
                                in 500..599 -> eventRepository.markSkipped(exportedEvent.id)
                                else -> onEventSentFailed(exportedEvent)
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    internal fun routeSendingEvent(exportedEvent: ExportedEvent): Call? {
        val integrationConfig: IntegrationConfig = when (exportedEvent.integrationConfiguration?.type) {
            IntegrationConfigType.PROJECT -> ProjectConfig(
                exportedEvent.integrationConfiguration.baseUrl,
                exportedEvent.integrationConfiguration.integrationId,
                exportedEvent.integrationConfiguration.authorization
            )

            IntegrationConfigType.STREAM -> StreamConfig(
                exportedEvent.integrationConfiguration.baseUrl,
                exportedEvent.integrationConfiguration.integrationId
            )

            else -> {
                configuration.integrationConfig.let { ic ->
                    // for older event in database without exponeaProject, fallback to current project config if available
                    if (exportedEvent.projectId != null && ic is ProjectConfig) {
                        ProjectConfig(
                            ic.baseUrl,
                            exportedEvent.projectId,
                            ic.authorization
                        )
                    } else {
                        // otherwise log error, the event can never be sent and is removed by the flush
                        Logger.e(
                            this,
                            "Event ${exportedEvent.id} cannot be sent, integration data is missing"
                        )
                        return null
                    }
                }
            }
        }

        val simpleEvent = Event(
                type = exportedEvent.type,
                timestamp = exportedEvent.timestamp,
                customerIds = exportedEvent.customerIds,
                properties = exportedEvent.properties
        )
        val isCurrentCustomerEvent = !hasMismatchedCustomerIds(exportedEvent)
        return exponeaService.let {
            when (exportedEvent.route) {
                Route.TRACK_EVENTS -> it.postEvent(integrationConfig, simpleEvent, isCurrentCustomerEvent)
                Route.TRACK_CUSTOMERS -> it.postCustomer(integrationConfig, simpleEvent, isCurrentCustomerEvent)
                Route.TRACK_CAMPAIGN -> it.postCampaignClick(integrationConfig, simpleEvent, isCurrentCustomerEvent)
                else -> {
                    Logger.e(
                        this,
                        "Event ${exportedEvent.id} cannot be sent, unknown route: ${exportedEvent.route}"
                    )
                    return null
                }
            }
        }
    }

    /**
     * Returns true when the event’s customerIds differ from the current customer.
     * If the event has no customerIds, returns false.
     */
    private fun hasMismatchedCustomerIds(exportedEvent: ExportedEvent): Boolean {
        val eventIds = exportedEvent.customerIds ?: return false
        val currentIds = customerIdsRepository.get().toHashMap()
        return currentIds != eventIds
    }

    @WorkerThread
    private fun onEventSentSuccess(exportedEvent: ExportedEvent) {
        Logger.d(this, "onEventSentSuccess: ${exportedEvent.id}")
        eventRepository.remove(exportedEvent.id)
        onEventUploaded(exportedEvent)
    }

    @WorkerThread
    private fun onEventSentFailed(exportedEvent: ExportedEvent) {
        val tries = exportedEvent.tries + 1
        if (tries >= configuration.maxTries) {
            Logger.w(this, "Event ${exportedEvent.id} removed, upload failed $tries times")
            eventRepository.remove(exportedEvent.id)
        } else {
            Logger.d(this, "Event ${exportedEvent.id} failed, attempt $tries of ${configuration.maxTries}")
            eventRepository.markFailed(exportedEvent.id)
        }
    }
}
