package com.exponea.sdk.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TokenTrackingTest() : ExponeaSDKTest() {
    lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkConstructor(EventManagerImpl::class)
        Exponea.flushMode = FlushMode.MANUAL
    }

    @Test
    fun `should track fcm token when Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration())
        Exponea.handleNewToken(context, "mock token")
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf("google_push_notification_id" to "mock token"),
                EventType.PUSH_TOKEN
            )
        }
    }

    @Test
    fun `should track fcm token after Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration())
        Exponea.handleNewToken(context, "mock token")
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf("google_push_notification_id" to "mock token"),
                EventType.PUSH_TOKEN
            )
        }
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should track hms token when Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration())
        Exponea.handleNewHmsToken(context, "mock token")
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf("huawei_push_notification_id" to "mock token"),
                EventType.PUSH_TOKEN
            )
        }
    }

    @Test
    fun `should track hms token after Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration())
        Exponea.handleNewHmsToken(context, "mock token")
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf("huawei_push_notification_id" to "mock token"),
                EventType.PUSH_TOKEN
            )
        }
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
    }
}
