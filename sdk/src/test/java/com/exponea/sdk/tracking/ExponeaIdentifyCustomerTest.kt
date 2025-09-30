package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.DeviceIdManager
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.TokenType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaIdentifyCustomerTest : ExponeaSDKTest() {
    @Before
    fun before() {
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        mockkConstructorFix(TelemetryManager::class)
        skipInstallEvent()
    }

    @Test
    fun `should identify customer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(projectToken = "mock-token", automaticSessionTracking = false)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.identifyCustomer(
            CustomerIds().withId("registered", "john@doe.com"),
            PropertiesList(hashMapOf("first_name" to "NewName"))
        )
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals(
            hashMapOf<String, Any>(
                "first_name" to "NewName",
                "application_id" to "default-application"
            ),
            eventSlot.captured.properties
        )
        assertEquals("john@doe.com", eventSlot.captured.customerIds?.get("registered"))
        assertEquals(EventType.TRACK_CUSTOMER, eventTypeSlot.captured)
    }

    @Test
    fun `should add default properties to track_customer by default`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            defaultProperties = hashMapOf(
                "def_key" to "def_value"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.identifyCustomer(
            CustomerIds().withId("registered", "john@doe.com"),
            PropertiesList(hashMapOf("first_name" to "NewName"))
        )
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals(
            hashMapOf<String, Any>(
                "first_name" to "NewName",
                "def_key" to "def_value",
                "application_id" to "default-application"
            ),
            eventSlot.captured.properties
        )
        assertEquals("john@doe.com", eventSlot.captured.customerIds?.get("registered"))
        assertEquals(EventType.TRACK_CUSTOMER, eventTypeSlot.captured)
    }

    @Test
    fun `should add default properties to track_customer if allowed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            allowDefaultCustomerProperties = true,
            defaultProperties = hashMapOf(
                "def_key" to "def_value"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.identifyCustomer(
            CustomerIds().withId("registered", "john@doe.com"),
            PropertiesList(hashMapOf("first_name" to "NewName"))
        )
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals(
            hashMapOf<String, Any>(
                "first_name" to "NewName",
                "def_key" to "def_value",
                "application_id" to "default-application"
            ),
            eventSlot.captured.properties
        )
        assertEquals("john@doe.com", eventSlot.captured.customerIds?.get("registered"))
        assertEquals(EventType.TRACK_CUSTOMER, eventTypeSlot.captured)
    }

    @Test
    fun `should NOT add default properties to trackPushToken() if allowed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            allowDefaultCustomerProperties = true,
            defaultProperties = hashMapOf(
                "def_key" to "def_value"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs

        val pushToken = "abcd"
        Exponea.trackPushToken(pushToken)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals(
            hashMapOf<String, Any>(
                "push_notification_token" to pushToken,
                "platform" to TokenType.FCM.selfCheckProperty,
                "application_id" to "default-application",
                "valid" to true,
                "description" to Constants.PushPermissionStatus.PERMISSION_GRANTED,
                "device_id" to DeviceIdManager.getDeviceId(context)
            ),
            eventSlot.captured.properties
        )
        assertEquals(EventType.PUSH_TOKEN, eventTypeSlot.captured)
    }

    @Test
    fun `should NOT add default properties to trackHmsPushToken() if allowed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            allowDefaultCustomerProperties = true,
            defaultProperties = hashMapOf(
                "def_key" to "def_value"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs

        val pushToken = "abcd"
        Exponea.trackHmsPushToken(pushToken)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals(
            hashMapOf<String, Any>(
                "push_notification_token" to pushToken,
                "platform" to TokenType.HMS.selfCheckProperty,
                "application_id" to "default-application",
                "valid" to true,
                "description" to Constants.PushPermissionStatus.PERMISSION_GRANTED,
                "device_id" to DeviceIdManager.getDeviceId(context)
            ),
            eventSlot.captured.properties
        )
        assertEquals(EventType.PUSH_TOKEN, eventTypeSlot.captured)
    }

    @Test
    fun `should NOT add default properties to track_customer if denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            allowDefaultCustomerProperties = false,
            defaultProperties = hashMapOf(
                "def_key" to "def_value"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.identifyCustomer(
            CustomerIds().withId("registered", "john@doe.com"),
            PropertiesList(hashMapOf("first_name" to "NewName"))
        )
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals(
            hashMapOf<String, Any>(
                "first_name" to "NewName",
                "application_id" to "default-application"
            ),
            eventSlot.captured.properties
        )
        assertEquals("john@doe.com", eventSlot.captured.customerIds?.get("registered"))
        assertEquals(EventType.TRACK_CUSTOMER, eventTypeSlot.captured)
    }

    @Test
    fun `should NOT add default properties to trackPushToken() if denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            allowDefaultCustomerProperties = false,
            defaultProperties = hashMapOf(
                "def_key" to "def_value"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs

        val pushToken = "abcd"
        Exponea.trackPushToken(pushToken)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals(
            hashMapOf<String, Any>(
                "push_notification_token" to pushToken,
                "platform" to TokenType.FCM.selfCheckProperty,
                "application_id" to "default-application",
                "valid" to true,
                "description" to Constants.PushPermissionStatus.PERMISSION_GRANTED,
                "device_id" to DeviceIdManager.getDeviceId(context)
            ),
            eventSlot.captured.properties
        )
        assertEquals(EventType.PUSH_TOKEN, eventTypeSlot.captured)
    }

    @Test
    fun `should NOT add default properties to trackHmsPushToken() if denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            allowDefaultCustomerProperties = false,
            defaultProperties = hashMapOf(
                "def_key" to "def_value"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs

        val pushToken = "abcd"
        Exponea.trackHmsPushToken(pushToken)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals(
            hashMapOf<String, Any>(
                "push_notification_token" to pushToken,
                "platform" to TokenType.HMS.selfCheckProperty,
                "application_id" to "default-application",
                "valid" to true,
                "description" to Constants.PushPermissionStatus.PERMISSION_GRANTED,
                "device_id" to DeviceIdManager.getDeviceId(context)
            ),
            eventSlot.captured.properties
        )
        assertEquals(EventType.PUSH_TOKEN, eventTypeSlot.captured)
    }

    @Test
    fun `should track identify telemetry`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(projectToken = "mock-token", automaticSessionTracking = false)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        mockkConstructorFix(TelemetryManager::class)
        val telemetryTelemetryEventSlot = slot<com.exponea.sdk.telemetry.model.TelemetryEvent>()
        val telemetryPropertiesSlot = slot<MutableMap<String, String>>()
        every {
            anyConstructed<TelemetryManager>().reportEvent(
                capture(telemetryTelemetryEventSlot),
                capture(telemetryPropertiesSlot)
            )
        } just Runs
        Exponea.telemetry = TelemetryManager(ApplicationProvider.getApplicationContext())
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        } just Runs
        Exponea.identifyCustomer(
            CustomerIds().withId("registered", "john@doe.com"),
            PropertiesList(hashMapOf("first_name" to "NewName"))
        )
        assertTrue(telemetryTelemetryEventSlot.isCaptured)
        val capturedEventType = telemetryTelemetryEventSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.IDENTIFY_CUSTOMER, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertTrue(capturedProps.isEmpty())
    }
}
