package com.exponea.sdk.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.FirebaseTokenRepositoryProvider
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaFirebaseMessageServiceTest() : ExponeaSDKTest() {
    lateinit var context: Context
    lateinit var service: ExponeaFirebaseMessageService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkConstructor(EventManagerImpl::class)
        Exponea.flushMode = FlushMode.MANUAL

        val controller = Robolectric.buildService(ExponeaFirebaseMessageService::class.java)
        service = controller.bind().get()
    }

    @Test
    fun `should track token when Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration())
        service.onNewToken("mock token")
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
    fun `should track token when Exponea can be auto-initialized`() {
        ExponeaConfigRepository.set(context, ExponeaConfiguration())
        service.onNewToken("mock token")
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
    fun `should track token after Exponea is initialized`() {
        service.onNewToken("mock token")
        assertEquals("mock token", FirebaseTokenRepositoryProvider.get(context).get())
        assertNull(FirebaseTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
        Exponea.init(context, ExponeaConfiguration())
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf("google_push_notification_id" to "mock token"),
                EventType.PUSH_TOKEN
            )
        }
        assertEquals("mock token", FirebaseTokenRepositoryProvider.get(context).get())
        assertNotNull(FirebaseTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
    }
}
