package com.exponea.sdk

import com.exponea.sdk.manager.InAppContentBlockManager
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.runOnMainThread
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The current local availability of an in-app content-block placeholder. */
sealed class InAppContentBlockAvailability {
    object Loading : InAppContentBlockAvailability()
    object Ready : InAppContentBlockAvailability()
    object Empty : InAppContentBlockAvailability()
}

/** Result of an [RuntimeInAppContentBlockController.availability] request. */
sealed class InAppContentBlockAvailabilityDecision {
    data class Resolved(val availability: InAppContentBlockAvailability) : InAppContentBlockAvailabilityDecision()
    object TimedOut : InAppContentBlockAvailabilityDecision()
}

/** Controls whether an invalidation immediately warms content again. */
enum class InAppContentBlockInvalidateMode { EAGER, LAZY }

/**
 * Imperative, callback-based runtime control surface for In-App Content Blocks.
 *
 * Content, rendering and tracking remain owned by the existing content-block manager and the
 * legacy placeholder view. Every callback is delivered once on the main thread.
 */
class RuntimeInAppContentBlockController internal constructor(
    private val manager: InAppContentBlockManager
) : OnIntegrationStoppedCallback {
    private data class LoadEntry(
        val callbacks: MutableList<(InAppContentBlockAvailability) -> Unit>,
        var forceRefresh: Boolean,
        var forceRefreshAfterCurrentLoad: Boolean = false,
        var startedGeneration: Long? = null
    )

    private data class InvalidationState(
        var generation: Long = 0,
        var evictionPending: Boolean = false,
        var refreshRequired: Boolean = false
    )

    private class AvailabilityWaiter(
        val callback: (InAppContentBlockAvailabilityDecision) -> Unit,
        var timeoutJob: Job? = null
    )

    private class PrefetchCollector(
        private val ids: List<String>,
        private val callback: (Map<String, InAppContentBlockAvailability>) -> Unit
    ) {
        private val lock = Any()
        private val results = linkedMapOf<String, InAppContentBlockAvailability>()
        private var completed = false
        var timeoutJob: Job? = null

        fun record(id: String, availability: InAppContentBlockAvailability) {
            val finalResult = synchronized(lock) {
                if (completed) return
                results[id] = availability
                if (results.size == ids.size) finishLocked() else null
            }
            finalResult?.let(::complete)
        }

        fun timeout() {
            val finalResult = synchronized(lock) {
                if (completed) return
                ids.forEach { id ->
                    if (!results.containsKey(id)) {
                        results[id] = InAppContentBlockAvailability.Empty
                    }
                }
                finishLocked()
            }
            complete(finalResult)
        }

        private fun finishLocked(): Map<String, InAppContentBlockAvailability> {
            completed = true
            return results.toMap()
        }

        private fun complete(result: Map<String, InAppContentBlockAvailability>) {
            timeoutJob?.cancel()
            callback(result)
        }
    }

    private val lock = Any()
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val states = mutableMapOf<String, InAppContentBlockAvailability>()
    private val inFlightLoads = mutableMapOf<String, LoadEntry>()
    private val availabilityWaiters = mutableMapOf<String, MutableList<AvailabilityWaiter>>()
    private val invalidations = mutableMapOf<String, InvalidationState>()
    /* Manager invalidation is synchronous, so serialize it without ever occupying a caller thread. */
    private val evictionLock = Any()
    @Volatile private var stopped = false

    /**
     * Prefetches supplied placeholders, coalescing concurrent work for the same placeholder.
     * When [deadlineMillis] is set, IDs still loading at that deadline are returned as Empty;
     * their shared background fetches continue warming the cache.
     */
    @JvmOverloads
    fun prefetch(
        ids: List<String>,
        deadlineMillis: Long? = null,
        callback: ((Map<String, InAppContentBlockAvailability>) -> Unit)? = null
    ) {
        val distinctIds = ids.distinct()
        if (stopped) {
            callback?.let { mainThreadOnce(it)(distinctIds.associateWith { InAppContentBlockAvailability.Empty }) }
            return
        }
        if (callback == null) {
            distinctIds.forEach { requestLoad(it, forceRefresh = false) {} }
        } else {
            val collector = PrefetchCollector(distinctIds, mainThreadOnce(callback))
            if (deadlineMillis != null && deadlineMillis <= 0) {
                // Take the result snapshot before requests replace cached terminal states with Loading.
                val cachedStates = synchronized(lock) {
                    distinctIds.mapNotNull { id ->
                        states[id]?.takeIf { it !== InAppContentBlockAvailability.Loading }?.let { id to it }
                    }
                }
                cachedStates.forEach { (id, state) -> collector.record(id, state) }
                distinctIds.forEach { requestLoad(it, forceRefresh = false) {} }
                collector.timeout()
                return
            }
            if (deadlineMillis != null) {
                collector.timeoutJob = workerScope.launch {
                    delay(deadlineMillis)
                    collector.timeout()
                }
            }
            if (distinctIds.isEmpty()) {
                collector.timeout()
            } else {
                distinctIds.forEach { id ->
                    requestLoad(id, forceRefresh = false) { collector.record(id, it) }
                }
            }
        }
    }

    /**
     * Invalidates only data owned by the supplied placeholders. Eager invalidation forces a
     * fresh manager request; lazy invalidation only evicts cache and leaves the IDs Loading.
     */
    @JvmOverloads
    fun invalidate(
        ids: List<String>,
        reason: String,
        mode: InAppContentBlockInvalidateMode = InAppContentBlockInvalidateMode.EAGER,
        callback: ((Map<String, InAppContentBlockAvailability>) -> Unit)? = null
    ) {
        val distinctIds = ids.distinct()
        val complete = callback?.let(::mainThreadOnce)
        if (stopped) {
            complete?.invoke(distinctIds.associateWith { InAppContentBlockAvailability.Empty })
            return
        }
        Logger.i(this, "InAppCB runtime invalidation: ${distinctIds.joinToString()} (reason=$reason)")
        val generations = synchronized(lock) {
            distinctIds.associateWith { id ->
                val state = invalidations.getOrPut(id) { InvalidationState() }
                state.generation++
                state.evictionPending = true
                states[id] = InAppContentBlockAvailability.Loading
                inFlightLoads[id]?.let {
                    it.forceRefreshAfterCurrentLoad = true
                    state.refreshRequired = true
                }
                state.generation
            }
        }
        if (mode == InAppContentBlockInvalidateMode.LAZY) {
            evictAsync(distinctIds, generations) {
                complete?.invoke(distinctIds.associateWith { InAppContentBlockAvailability.Loading })
            }
            return
        }
        if (complete == null) {
            distinctIds.forEach { requestLoad(it, forceRefresh = true) {} }
        } else {
            completeAll(distinctIds, complete) { id, done -> requestLoad(id, forceRefresh = true, done) }
        }
        evictAsync(distinctIds, generations)
    }

    /**
     * Resolves cached availability immediately, otherwise waits no longer than [deadlineMillis].
     * A timeout only ends this callback's wait: the shared background load continues.
     */
    fun availability(
        id: String,
        deadlineMillis: Long,
        callback: (InAppContentBlockAvailabilityDecision) -> Unit
    ) {
        val complete = mainThreadOnce(callback)
        if (stopped) {
            complete(InAppContentBlockAvailabilityDecision.TimedOut)
            return
        }
        val current = synchronized(lock) { states[id] ?: InAppContentBlockAvailability.Loading }
        if (current !== InAppContentBlockAvailability.Loading) {
            complete(InAppContentBlockAvailabilityDecision.Resolved(current))
            return
        }
        if (deadlineMillis <= 0) {
            complete(InAppContentBlockAvailabilityDecision.TimedOut)
            requestLoad(id, forceRefresh = false) {}
            return
        }

        val waiter = AvailabilityWaiter(mainThreadOnce { complete(it) })
        synchronized(lock) {
            if (stopped) {
                waiter.callback(InAppContentBlockAvailabilityDecision.TimedOut)
                return
            }
            availabilityWaiters.getOrPut(id) { mutableListOf() }.add(waiter)
        }
        waiter.timeoutJob = workerScope.launch {
            delay(deadlineMillis)
            val didTimeout = synchronized(lock) { availabilityWaiters[id]?.remove(waiter) == true }
            if (didTimeout) waiter.callback(InAppContentBlockAvailabilityDecision.TimedOut)
        }
        requestLoad(id, forceRefresh = false) {}
    }

    private fun completeAll(
        ids: List<String>,
        complete: (Map<String, InAppContentBlockAvailability>) -> Unit,
        request: (String, (InAppContentBlockAvailability) -> Unit) -> Unit
    ) {
        if (ids.isEmpty()) {
            complete(emptyMap())
            return
        }
        val resultLock = Any()
        val results = linkedMapOf<String, InAppContentBlockAvailability>()
        ids.forEach { id ->
            request(id) { availability ->
                val completed = synchronized(resultLock) {
                    results[id] = availability
                    if (results.size == ids.size) results.toMap() else null
                }
                completed?.let(complete)
            }
        }
    }

    private fun requestLoad(
        id: String,
        forceRefresh: Boolean,
        callback: (InAppContentBlockAvailability) -> Unit
    ) {
        var startLoad: Boolean? = null
        var stoppedResult = false
        synchronized(lock) {
            if (stopped) {
                stoppedResult = true
            } else {
                val current = inFlightLoads[id]
                val invalidation = invalidations.getOrPut(id) { InvalidationState() }
                if (invalidation.evictionPending && !forceRefresh) {
                    // A caller arriving during invalidation must not attach to stale cache work.
                    invalidation.refreshRequired = true
                }
                if (current == null) {
                    inFlightLoads[id] = LoadEntry(mutableListOf(callback), forceRefresh)
                    if (!invalidation.evictionPending) startLoad = forceRefresh || invalidation.refreshRequired
                } else {
                    current.callbacks.add(callback)
                    if ((forceRefresh || invalidation.refreshRequired) && !current.forceRefresh) {
                        current.forceRefreshAfterCurrentLoad = true
                    }
                }
            }
        }
        if (stoppedResult) {
            callback(InAppContentBlockAvailability.Empty)
            return
        }
        startLoad?.let { startWithLoading(id, it) }
    }

    private fun startWithLoading(id: String, forceRefresh: Boolean) {
        val generation = synchronized(lock) {
            states[id] = InAppContentBlockAvailability.Loading
            val entry = inFlightLoads[id] ?: return
            val invalidation = invalidations.getOrPut(id) { InvalidationState() }
            if (invalidation.evictionPending) return
            entry.startedGeneration = invalidation.generation
            entry.forceRefresh = forceRefresh
            invalidation.generation
        }
        workerScope.launch {
            runCatching {
                manager.loadPlaceholderAsync(
                    placeholderId = id,
                    forceRefresh = forceRefresh,
                    completion = { successfulLoad -> completeLoad(id, successfulLoad, generation) }
                )
            }.onFailure {
                Logger.e(this@RuntimeInAppContentBlockController, "InAppCB runtime load failed for $id", it)
                completeLoad(id, false, generation)
            }
        }
    }

    private fun completeLoad(id: String, successfulLoad: Boolean, loadGeneration: Long) {
        if (stopped) return
        val result = if (successfulLoad && manager.hasRenderableContent(id)) {
            InAppContentBlockAvailability.Ready
        } else {
            InAppContentBlockAvailability.Empty
        }
        var deferTerminalResult = false
        val forceAnotherLoad = synchronized(lock) {
            val entry = inFlightLoads[id] ?: return
            val invalidation = invalidations.getOrPut(id) { InvalidationState() }
            if (entry.startedGeneration != loadGeneration) return
            if (entry.forceRefreshAfterCurrentLoad || loadGeneration != invalidation.generation) {
                entry.forceRefreshAfterCurrentLoad = false
                entry.forceRefresh = true
                invalidation.refreshRequired = true
                deferTerminalResult = true
                !invalidation.evictionPending
            } else {
                false
            }
        }
        if (deferTerminalResult) {
            if (forceAnotherLoad) startWithLoading(id, forceRefresh = true)
            return
        }
        val callbacks = synchronized(lock) { inFlightLoads.remove(id)?.callbacks.orEmpty() }
        synchronized(lock) { invalidations[id]?.refreshRequired = false }
        setState(id, result)
        callbacks.forEach { it(result) }
        resolveAvailability(id, InAppContentBlockAvailabilityDecision.Resolved(result))
    }

    private fun setState(id: String, state: InAppContentBlockAvailability) = synchronized(lock) {
        states[id] = state
    }

    private fun evictAsync(ids: List<String>, generations: Map<String, Long>, afterEviction: (() -> Unit)? = null) {
        workerScope.launch {
            runCatching {
                synchronized(evictionLock) { manager.invalidatePlaceholders(ids) }
            }.onFailure { Logger.e(this@RuntimeInAppContentBlockController, "InAppCB runtime invalidation failed", it) }
            ids.forEach { id -> finishEviction(id, generations[id] ?: return@forEach) }
            afterEviction?.invoke()
        }
    }

    private fun finishEviction(id: String, generation: Long) {
        var start = false
        synchronized(lock) {
            if (stopped) return
            val invalidation = invalidations[id] ?: return
            // A later invalidation owns the pending flag and will decide what to start.
            if (invalidation.generation != generation) return
            invalidation.evictionPending = false
            val entry = inFlightLoads[id]
            if (entry != null && entry.startedGeneration != generation) {
                start = true
            }
        }
        if (start) startWithLoading(id, forceRefresh = true)
    }

    private fun resolveAvailability(id: String, decision: InAppContentBlockAvailabilityDecision) {
        val waiters = synchronized(lock) { availabilityWaiters.remove(id).orEmpty() }
        waiters.forEach {
            it.timeoutJob?.cancel()
            it.callback(decision)
        }
    }

    private fun <T> mainThreadOnce(callback: (T) -> Unit): (T) -> Unit {
        val delivered = AtomicBoolean(false)
        return { value ->
            if (delivered.compareAndSet(false, true)) {
                runOnMainThread { callback(value) }
            }
        }
    }

    /** Identity reset invalidates the controller's local availability snapshot. */
    internal fun onAnonymized() = synchronized(lock) { states.clear() }

    override fun onIntegrationStopped() {
        val callbacks: List<(InAppContentBlockAvailability) -> Unit>
        val waiters: List<AvailabilityWaiter>
        synchronized(lock) {
            if (stopped) return
            stopped = true
            callbacks = inFlightLoads.values.flatMap { it.callbacks }
            waiters = availabilityWaiters.values.flatten()
            inFlightLoads.clear()
            availabilityWaiters.clear()
            states.clear()
        }
        callbacks.forEach { it(InAppContentBlockAvailability.Empty) }
        waiters.forEach {
            it.timeoutJob?.cancel()
            it.callback(InAppContentBlockAvailabilityDecision.TimedOut)
        }
        workerScope.cancel()
    }
}
