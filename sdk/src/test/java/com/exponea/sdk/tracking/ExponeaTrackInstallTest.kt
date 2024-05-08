package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaTrackInstallTest : ExponeaSDKTest() {
    lateinit var context: Context
    lateinit var configuration: ExponeaConfiguration
    @Before
    fun before() {
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        context = ApplicationProvider.getApplicationContext<Context>()
        configuration = ExponeaConfiguration(projectToken = "mock-token", automaticSessionTracking = false)
        Exponea.flushMode = FlushMode.MANUAL
    }

    @Test
    fun `should track install event`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.init(context, configuration)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals("installation", eventSlot.captured.type)
        assertEquals(EventType.INSTALL, eventTypeSlot.captured)
    }

    @Test
    fun `should only track install event once`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.init(context, configuration)
        Exponea.trackInstallEvent()
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
    }
}
