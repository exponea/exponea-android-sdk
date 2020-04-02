package com.exponea.sdk.manager

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EventManagerTest : ExponeaSDKTest() {
    lateinit var eventRepo: EventRepository
    lateinit var flushManager: FlushManager
    lateinit var inAppMessageManager: InAppMessageManager
    lateinit var manager: EventManagerImpl

    lateinit var addedEvents: ArrayList<DatabaseStorageObject<ExportedEventType>>

    fun setup(configuration: ExponeaConfiguration, flushMode: FlushMode) {
        mockkObject(Exponea)
        every { Exponea.flushMode } returns flushMode

        eventRepo = mockk()
        addedEvents = arrayListOf()
        every { eventRepo.add(capture(addedEvents)) } returns true

        val customerIdsRepo = mockk<CustomerIdsRepository>()
        every { customerIdsRepo.get() } returns CustomerIds(cookie = "mock-cookie")

        flushManager = mockk()
        every { flushManager.flushData(any()) } just Runs

        inAppMessageManager = mockk()
        every { inAppMessageManager.showRandom(any(), any(), any(), any()) } returns null
        every { inAppMessageManager.sessionStarted(any()) } just Runs

        manager = EventManagerImpl(
            ApplicationProvider.getApplicationContext(),
            configuration,
            eventRepo,
            customerIdsRepo,
            flushManager,
            inAppMessageManager
        )
    }

    @Test
    fun `should track event`() {
        setup(ExponeaConfiguration(projectToken = "mock-project-token"), FlushMode.MANUAL)
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        verify {
            eventRepo.add(any())
            inAppMessageManager.showRandom(any(), any(), any(), any())
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
        assertEquals(
            ExportedEventType(
                "test-event",
                123.0,
                hashMapOf("cookie" to "mock-cookie"),
                hashMapOf("prop" to "value")
            ),
            addedEvents.first().item
        )
        assertEquals(
            ExponeaProject("https://api.exponea.com", "mock-project-token", null),
            addedEvents.first().exponeaProject
        )
    }

    @Test
    fun `should track event for all projects`() {
        setup(
            ExponeaConfiguration(
                projectToken = "mock-project-token",
                projectRouteMap = hashMapOf(EventType.INSTALL to arrayListOf(
                    ExponeaProject("mock_base_url1.com", "token1", "mock_auth"),
                    ExponeaProject("mock_base_url2.com", "token2", "mock_auth")
                ))
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.INSTALL)
        verify {
            eventRepo.add(any())
            inAppMessageManager.showRandom(any(), any(), any(), any())
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
        assertEquals(3, addedEvents.size)
        assertEquals(
            ExponeaProject("https://api.exponea.com", "mock-project-token", null),
            addedEvents[0].exponeaProject
        )
        assertEquals(
            ExponeaProject("mock_base_url1.com", "token1", "mock_auth"),
            addedEvents[1].exponeaProject
        )
        assertEquals(
            ExponeaProject("mock_base_url2.com", "token2", "mock_auth"),
            addedEvents[2].exponeaProject
        )
    }

    @Test
    fun `should start flush in immediate flush mode`() {
        setup(ExponeaConfiguration(projectToken = "mock-project-token"), FlushMode.IMMEDIATE)
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        verify {
            eventRepo.add(any())
            flushManager.flushData(any())
            inAppMessageManager.showRandom(any(), any(), any(), any())
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
    }

    @Test
    fun `should notify in-app message manager of session start`() {
        setup(ExponeaConfiguration(projectToken = "mock-project-token"), FlushMode.MANUAL)
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.SESSION_START)
        verify {
            eventRepo.add(any())
            inAppMessageManager.sessionStarted(any())
            inAppMessageManager.showRandom(any(), any(), any(), any())
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
    }

    @Test
    fun `should add default properties`() {
        setup(
            ExponeaConfiguration(
                projectToken = "mock-project-token",
                defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2")
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        assertEquals(
            ExportedEventType(
                "test-event",
                123.0,
                hashMapOf("cookie" to "mock-cookie"),
                hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2")
            ),
            addedEvents.first().item
        )
    }

    @Test
    fun `should not accumulate default properties`() {
        setup(
            ExponeaConfiguration(
                projectToken = "mock-project-token",
                defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2")
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        assertEquals(
            ExportedEventType(
                "test-event",
                123.0,
                hashMapOf("cookie" to "mock-cookie"),
                hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2")
            ),
            addedEvents[0].item
        )
        assertEquals(
            ExportedEventType(
                "test-event",
                123.0,
                hashMapOf("cookie" to "mock-cookie"),
                hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2")
            ),
            addedEvents[1].item
        )
    }
}
