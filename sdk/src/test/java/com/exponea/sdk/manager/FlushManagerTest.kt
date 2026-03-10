package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.Route
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FlushManagerTest : ExponeaSDKTest() {
    private lateinit var manager: FlushManager
    private lateinit var repo: EventRepository
    private lateinit var connectionManager: ConnectionManager
    private lateinit var service: ExponeaService

    private fun setup(connected: Boolean, serviceSuccess: Boolean) {
        val configuration = ExponeaConfiguration()
        val context = ApplicationProvider.getApplicationContext<Context>()

        connectionManager = mockk()
        every { connectionManager.isConnectedToInternet() } returns connected
        service = spyk(ExponeaMockService(serviceSuccess))
        repo = EventRepositoryImpl(context)
        repo.clear()
        manager = FlushManagerImpl(configuration, repo, service, connectionManager, {})
    }

    private fun createTestEvent(includeProject: Boolean, type: String? = "test_event"): ExportedEvent {
        val event = ExportedEvent(
                projectId = "old-project-id",
                type = type,
                timestamp = System.currentTimeMillis() / 1000.0,
                customerIds = hashMapOf(),
                properties = hashMapOf("property" to "value"),
                route = Route.TRACK_EVENTS,
                exponeaProject = if (includeProject)
                    ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth")
                else null
        )
        repo.add(event)
        return event
    }

    @Test
    fun `should flush event`() {
        setup(connected = true, serviceSuccess = true)
        createTestEvent(true)
        waitForIt {
            manager.flushData { _ ->
                it.assertEquals(0, repo.all().size)
                it()
            }
        }
        verify {
            service.postEvent(ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth"), any())
        }
    }

    @Test
    fun `should flush old event without exponea project`() {
        setup(connected = true, serviceSuccess = true)
        createTestEvent(false)
        waitForIt {
            manager.flushData { _ ->
                it.assertEquals(0, repo.all().size)
                it()
            }
        }
        verify {
            service.postEvent(ExponeaProject("https://api.exponea.com", "old-project-id", null), any())
        }
    }

    @Test
    fun `should fail to flush without internet connection`() {
        setup(connected = false, serviceSuccess = true)
        createTestEvent(true)
        every { connectionManager.isConnectedToInternet() } returns false
        waitForIt {
            manager.flushData { _ ->
                it.assertEquals(1, repo.all().size)
                it.assertEquals(0, repo.all().first().tries)
                it()
            }
        }
    }

    @Test
    fun `should increase tries on flush failure`() {
        setup(connected = true, serviceSuccess = false)
        createTestEvent(true)

        waitForIt {
            manager.flushData { _ ->
                assertEquals(1, repo.all().size)
                assertEquals(1, repo.all().first().tries)
                it()
            }
        }
    }

    @Test
    fun `should delete event on max tries reached`() {
        setup(connected = true, serviceSuccess = false)
        createTestEvent(true)

        val event = repo.all().first()
        event.tries = 10
        repo.update(event)

        waitForIt {
            manager.flushData { _ ->
                assertEquals(0, repo.all().size)
                it()
            }
        }
    }

    @Test
    fun `should only flush once`() {
        Exponea.loggerLevel = Logger.Level.VERBOSE
        setup(connected = true, serviceSuccess = false)
        createTestEvent(true)

        val testRunMaxMillis = 10000L
        val flushInvokeTries = 10
        // counts down once per callback: 1 for the first flush + flushInvokeTries for queued ones
        val allCallbacksDone = CountDownLatch(1 + flushInvokeTries)
        val firstFlushBlocking = CountDownLatch(1)
        val unblockFirstFlush = CountDownLatch(1)

        every { connectionManager.isConnectedToInternet() } answers {
            firstFlushBlocking.countDown()
            assertTrue(unblockFirstFlush.await(testRunMaxMillis, TimeUnit.MILLISECONDS))
            true
        }

        // Start the first flush — it will block inside isConnectedToInternet
        thread(start = true) {
            manager.flushData {
                assertTrue(it.isFailure)
                allCallbacksDone.countDown()
            }
        }

        // Wait until the first flush is blocking in isConnectedToInternet
        assertTrue(firstFlushBlocking.await(testRunMaxMillis, TimeUnit.MILLISECONDS))

        // Start concurrent flushes — their callbacks are queued, not fired immediately
        for (i in 1..flushInvokeTries) {
            thread(start = true) {
                manager.flushData {
                    // Callback fires when the ongoing flush finishes, not with "already in progress"
                    assertTrue(it.isFailure)
                    assertNotEquals("Flushing already in progress", it.exceptionOrNull()?.localizedMessage)
                    allCallbacksDone.countDown()
                }
            }
        }

        // Give concurrent threads time to queue their callbacks before releasing the first flush
        Thread.sleep(200)

        // Unblock the first flush
        unblockFirstFlush.countDown()

        // Wait for all callbacks (first flush + queued) to fire
        assertTrue(allCallbacksDone.await(testRunMaxMillis, TimeUnit.MILLISECONDS))

        // Verify that only one network request was made
        verify(exactly = 1) {
            service.postEvent(any(), any())
        }
    }

    @Test
    fun `should not hang when event has invalid route`() {
        setup(connected = true, serviceSuccess = true)
        val event = ExportedEvent(
            projectId = "pid",
            type = "test",
            timestamp = 0.0,
            customerIds = hashMapOf(),
            properties = hashMapOf(),
            route = null,
            exponeaProject = ExponeaProject("url", "token", "auth")
        )
        repo.add(event)
        val spyManager = spyk(manager as FlushManagerImpl)
        every { spyManager.routeSendingEvent(any()) } returns null
        waitForIt {
            spyManager.flushData { _ ->
                it.assertEquals(0, repo.all().size)
                it()
            }
        }
    }

    @Test
    fun `should post timestamp when tracking events`() {
        setup(connected = true, serviceSuccess = true)
        createTestEvent(true)
        waitForIt {
            manager.flushData { _ ->
                it.assertEquals(0, repo.all().size)
                it()
            }
        }
        val eventSlot = slot<Event>()
        verify {
            service.postEvent(
                ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth"),
                capture(eventSlot)
            )
        }
        assertNotNull(eventSlot.captured.timestamp)
    }

    @Test
    fun `should post timestamp when tracking push notifications`() {
        setup(connected = true, serviceSuccess = true)
        createTestEvent(true, type = Constants.EventTypes.push)
        waitForIt {
            manager.flushData { _ ->
                it.assertEquals(0, repo.all().size)
                it()
            }
        }
        val eventSlot = slot<Event>()
        verify {
            service.postEvent(
                ExponeaProject("mock_base_url.com", "mock_project_token", "mock_auth"),
                capture(eventSlot)
            )
        }
        assertNotNull(eventSlot.captured.timestamp)
    }
}
