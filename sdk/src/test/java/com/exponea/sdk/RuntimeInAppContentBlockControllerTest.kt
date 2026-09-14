package com.exponea.sdk

import android.os.Looper
import com.exponea.sdk.manager.InAppContentBlockManager
import com.exponea.sdk.testutil.runInSingleThread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RuntimeInAppContentBlockControllerTest {

    @Test
    fun `prefetch returns distinct final states and coalesces loads`() = runInSingleThread { idle ->
        val harness = ControllerHarness()
        val results = mutableListOf<Map<String, InAppContentBlockAvailability>>()
        var deliveredOnMainThread = false

        harness.controller.prefetch(listOf("hero", "hero", "footer")) {
            deliveredOnMainThread = Looper.myLooper() == Looper.getMainLooper()
            results.add(it)
        }

        harness.awaitLoad("hero")
        harness.awaitLoad("footer")
        assertEquals(1, harness.loads["hero"]?.size)
        assertEquals(1, harness.loads["footer"]?.size)
        harness.complete("hero", true)
        harness.complete("footer", false)
        idle()

        assertEquals(1, results.size)
        assertEquals(InAppContentBlockAvailability.Ready, results.single()["hero"])
        assertEquals(InAppContentBlockAvailability.Empty, results.single()["footer"])
        assertTrue(deliveredOnMainThread)
    }

    @Test
    fun `concurrent prefetches share one manager load`() = runInSingleThread { idle ->
        val harness = ControllerHarness()
        val callbacks = AtomicInteger()

        harness.controller.prefetch(listOf("hero")) { callbacks.incrementAndGet() }
        harness.controller.prefetch(listOf("hero")) { callbacks.incrementAndGet() }

        harness.awaitLoad("hero")
        assertEquals(1, harness.loads["hero"]?.size)
        harness.complete("hero", true)
        idle()

        assertEquals(2, callbacks.get())
    }

    @Test
    fun `prefetch deadline returns unresolved IDs as Empty while loading continues`() = runInSingleThread { idle ->
        val harness = ControllerHarness()
        var result: Map<String, InAppContentBlockAvailability>? = null

        harness.controller.prefetch(listOf("ready", "pending"), deadlineMillis = 0) { result = it }
        idle()

        harness.awaitLoad("ready")
        harness.awaitLoad("pending")
        assertEquals(
            mapOf("ready" to InAppContentBlockAvailability.Empty, "pending" to InAppContentBlockAvailability.Empty),
            result
        )
        assertEquals(1, harness.loads["ready"]?.size)
        assertEquals(1, harness.loads["pending"]?.size)
        harness.complete("ready", true)
        harness.controller.availability("ready", 1_000) { resolved ->
            assertEquals(
                InAppContentBlockAvailability.Ready,
                (resolved as InAppContentBlockAvailabilityDecision.Resolved).availability
            )
        }
        idle()
    }

    @Test
    fun `prefetch zero deadline returns cached terminal states while warming every ID`() = runInSingleThread { idle ->
        val harness = ControllerHarness()
        harness.controller.prefetch(listOf("hero")) { }
        harness.awaitLoad("hero")
        harness.complete("hero", true)
        idle()

        var result: Map<String, InAppContentBlockAvailability>? = null
        harness.controller.prefetch(listOf("hero", "footer"), deadlineMillis = 0) { result = it }
        idle()

        assertEquals(
            mapOf(
                "hero" to InAppContentBlockAvailability.Ready,
                "footer" to InAppContentBlockAvailability.Empty
            ),
            result
        )
        harness.awaitLoad("hero", 2)
        harness.awaitLoad("footer")
    }

    @Test
    fun `prefetch negative deadline returns cached terminal states while warming every ID`() =
        runInSingleThread { idle ->
            val harness = ControllerHarness()
            harness.controller.prefetch(listOf("hero")) { }
            harness.awaitLoad("hero")
            harness.complete("hero", true)
            idle()

            var result: Map<String, InAppContentBlockAvailability>? = null
            harness.controller.prefetch(listOf("hero", "footer"), deadlineMillis = -1) { result = it }
            idle()

            assertEquals(
                mapOf(
                    "hero" to InAppContentBlockAvailability.Ready,
                    "footer" to InAppContentBlockAvailability.Empty
                ),
                result
            )
            harness.awaitLoad("hero", 2)
            harness.awaitLoad("footer")
        }

    @Test
    fun `eager invalidation forces a refresh and reports its final state`() = runInSingleThread { idle ->
        val harness = ControllerHarness()
        var result: Map<String, InAppContentBlockAvailability>? = null

        harness.controller.invalidate(listOf("hero"), "cms changed") { result = it }

        harness.awaitInvalidation()
        verify(harness.manager).invalidatePlaceholders(listOf("hero"))
        harness.awaitLoad("hero")
        assertEquals(1, harness.loads["hero"]?.size)
        assertTrue(harness.forceRefreshes.single())
        harness.complete("hero", true)
        idle()

        assertEquals(InAppContentBlockAvailability.Ready, result?.get("hero"))
    }

    @Test
    fun `lazy invalidation completes Loading without requesting content`() = runInSingleThread { idle ->
        val harness = ControllerHarness()
        var result: Map<String, InAppContentBlockAvailability>? = null

        harness.controller.invalidate(listOf("hero", "hero"), "cms changed", InAppContentBlockInvalidateMode.LAZY) {
            result = it
        }
        harness.awaitInvalidation()
        Thread.sleep(25)
        idle()

        verify(harness.manager).invalidatePlaceholders(listOf("hero"))
        assertTrue(harness.loads.isEmpty())
        assertEquals(mapOf("hero" to InAppContentBlockAvailability.Loading), result)
    }

    @Test
    fun `lazy invalidation fences a running load and resolves all callers from one refresh`() =
        runInSingleThread { idle ->
            val harness = ControllerHarness(blockEviction = true)
            var prefetch: Map<String, InAppContentBlockAvailability>? = null
            var availability: InAppContentBlockAvailabilityDecision? = null

            harness.controller.prefetch(listOf("hero")) { prefetch = it }
            harness.awaitLoad("hero")
            harness.controller.invalidate(listOf("hero"), "cms changed", InAppContentBlockInvalidateMode.LAZY)
            harness.awaitInvalidation()
            harness.controller.availability("hero", 10_000) { availability = it }

            harness.complete("hero", true)
            harness.releaseEviction()
            harness.awaitLoad("hero", 2)
            harness.complete("hero", true)
            idle()

            assertEquals(InAppContentBlockAvailability.Ready, prefetch?.get("hero"))
            assertEquals(
                InAppContentBlockAvailability.Ready,
                (availability as InAppContentBlockAvailabilityDecision.Resolved).availability
            )
            assertEquals(listOf(false, true), harness.forceRefreshes)
        }

    @Test
    fun `availability resolves cached state and zero deadline keeps loading in background`() =
        runInSingleThread { idle ->
            val harness = ControllerHarness()
            harness.controller.prefetch(listOf("cached")) { }
            harness.awaitLoad("cached")
            harness.complete("cached", true)
            var cached: InAppContentBlockAvailabilityDecision? = null
            harness.controller.availability("cached", 0) { cached = it }

            var timedOut: InAppContentBlockAvailabilityDecision? = null
            harness.controller.availability("pending", 0) { timedOut = it }
            var negativeTimedOut: InAppContentBlockAvailabilityDecision? = null
            harness.controller.availability("negative", -1) { negativeTimedOut = it }
            var resolvedBeforeDeadline: InAppContentBlockAvailabilityDecision? = null
            harness.controller.availability("soon", 10_000) { resolvedBeforeDeadline = it }
            harness.awaitLoad("pending")
            harness.awaitLoad("negative")
            harness.awaitLoad("soon")
            harness.complete("soon", true)
            idle()

            assertTrue(cached is InAppContentBlockAvailabilityDecision.Resolved)
            assertEquals(
                InAppContentBlockAvailability.Ready,
                (cached as InAppContentBlockAvailabilityDecision.Resolved).availability
            )
            assertEquals(InAppContentBlockAvailabilityDecision.TimedOut, timedOut)
            assertEquals(InAppContentBlockAvailabilityDecision.TimedOut, negativeTimedOut)
            assertEquals(
                InAppContentBlockAvailability.Ready,
                (resolvedBeforeDeadline as InAppContentBlockAvailabilityDecision.Resolved).availability
            )
            assertEquals(1, harness.loads["pending"]?.size)
            assertEquals(1, harness.loads["negative"]?.size)
            harness.complete("pending", true)
        }

    @Test
    fun `stopping resolves pending prefetch with Empty and availability with TimedOut`() = runInSingleThread { idle ->
        val harness = ControllerHarness()
        var prefetch: Map<String, InAppContentBlockAvailability>? = null
        var availability: InAppContentBlockAvailabilityDecision? = null

        harness.controller.prefetch(listOf("hero")) { prefetch = it }
        harness.controller.availability("footer", 10_000) { availability = it }
        harness.controller.onIntegrationStopped()
        idle()

        assertEquals(mapOf("hero" to InAppContentBlockAvailability.Empty), prefetch)
        assertEquals(InAppContentBlockAvailabilityDecision.TimedOut, availability)
    }

    @Test
    fun `prefetch invokes manager load away from the main thread`() = runInSingleThread {
        val harness = ControllerHarness()

        assertEquals(Looper.getMainLooper(), Looper.myLooper())
        harness.controller.prefetch(listOf("hero"))
        harness.awaitLoad("hero")

        assertTrue(harness.loadThreads.single() !== Looper.getMainLooper().thread)
    }

    private class ControllerHarness(private val blockEviction: Boolean = false) {
        val loads = linkedMapOf<String, MutableList<(Boolean) -> Unit>>()
        val forceRefreshes = mutableListOf<Boolean>()
        val loadThreads = mutableListOf<Thread>()
        private val loadMonitor = Object()
        private val loadInvocations = mutableMapOf<String, Int>()
        private val invalidated = CountDownLatch(1)
        private val evictionRelease = CountDownLatch(if (blockEviction) 1 else 0)
        val manager: InAppContentBlockManager = mock {
            on { hasRenderableContent(any()) } doReturn true
            on { invalidatePlaceholders(any()) } doAnswer {
                invalidated.countDown()
                evictionRelease.await(2, TimeUnit.SECONDS)
                Unit
            }
            on { loadPlaceholderAsync(any(), any(), any()) } doAnswer { invocation ->
                val id = invocation.getArgument<String>(0)
                val completion = invocation.getArgument<(Boolean) -> Unit>(2)
                synchronized(loadMonitor) {
                    forceRefreshes += invocation.getArgument<Boolean>(1)
                    loadThreads += Thread.currentThread()
                    loadInvocations[id] = (loadInvocations[id] ?: 0) + 1
                    loads.getOrPut(id) { mutableListOf() }.add(completion)
                    loadMonitor.notifyAll()
                }
                Unit
            }
        }
        val controller = RuntimeInAppContentBlockController(manager)

        fun complete(id: String, successful: Boolean) {
            val completion = synchronized(loadMonitor) {
                loads[id]?.takeIf { it.isNotEmpty() }?.removeAt(0)
            }
            completion?.invoke(successful)
        }

        fun awaitInvalidation() = assertTrue(invalidated.await(2, TimeUnit.SECONDS))

        fun releaseEviction() = evictionRelease.countDown()

        fun awaitLoad(id: String, expectedCount: Int = 1) = synchronized(loadMonitor) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            while ((loadInvocations[id] ?: 0) < expectedCount) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) break
                TimeUnit.NANOSECONDS.timedWait(loadMonitor, remaining)
            }
            assertTrue((loadInvocations[id] ?: 0) >= expectedCount)
        }
    }
}
