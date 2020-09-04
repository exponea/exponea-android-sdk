package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaIdentifyCustomerTest : ExponeaSDKTest() {
    @Before
    fun before() {
        mockkConstructor(EventManagerImpl::class)
        skipInstallEvent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(projectToken = "mock-token", automaticSessionTracking = false)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
    }

    @Test
    fun `should identify customer`() {
        val eventSlot = slot<ExportedEventType>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot))
        } just Runs
        Exponea.identifyCustomer(
            CustomerIds().withId("registered", "john@doe.com"),
            PropertiesList(hashMapOf("first_name" to "NewName"))
        )
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any())
        }

        assertEquals(
            hashMapOf("first_name" to "NewName") as HashMap<String, Any>,
            eventSlot.captured.properties
        )
        assertEquals("john@doe.com", eventSlot.captured.customerIds?.get("registered"))
        assertEquals(EventType.TRACK_CUSTOMER, eventTypeSlot.captured)
    }
}
