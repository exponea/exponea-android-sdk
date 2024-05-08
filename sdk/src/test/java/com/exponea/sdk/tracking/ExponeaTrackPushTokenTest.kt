package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.TokenType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaTrackPushTokenTest : ExponeaSDKTest() {
    @Before
    fun before() {
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        skipInstallEvent()
    }

    private fun initSdk(automaticPushNotification: Boolean = true) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            automaticPushNotification = automaticPushNotification
        )
        Exponea.flushMode = MANUAL
        Exponea.init(context, configuration)
    }

    @Test
    fun `should track FCM push token after SDK init`() {
        initSdk()
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.trackPushToken(token = "test-google-push-token")
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals("campaign", eventSlot.captured.type)
        assertEquals(
            hashMapOf<String, Any>("google_push_notification_id" to "test-google-push-token"),
            eventSlot.captured.properties
        )
        assertEquals(EventType.PUSH_TOKEN, eventTypeSlot.captured)
    }

    @Test
    fun `should track FCM push token after SDK init with disabled automaticPushNotification`() {
        initSdk(false)
        Exponea.trackPushToken(token = "test-google-push-token")
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
    }

    @Test
    fun `should track push HMS token after SDK init`() {
        initSdk()
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.trackHmsPushToken(token = "test-google-push-token")
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }

        assertEquals("campaign", eventSlot.captured.type)
        assertEquals(
            hashMapOf<String, Any>("huawei_push_notification_id" to "test-google-push-token"),
            eventSlot.captured.properties
        )
        assertEquals(EventType.PUSH_TOKEN, eventTypeSlot.captured)
    }

    @Test
    fun `should track HMS push token after SDK init with disabled automaticPushNotification`() {
        initSdk(false)
        Exponea.trackHmsPushToken(token = "test-google-push-token")
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
    }

    @Test
    fun `should store FCM push token after SDK init`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        initSdk()
        val pushToken = "test-google-push-token"
        Exponea.trackPushToken(token = pushToken)
        assertNotNull(tokenRepository.get())
        assertEquals(pushToken, tokenRepository.get())
        assertEquals(TokenType.FCM, tokenRepository.getLastTokenType())
    }

    @Test
    fun `should store HMS push token after SDK init`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        initSdk()
        val pushToken = "test-google-push-token"
        Exponea.trackHmsPushToken(token = pushToken)
        assertNotNull(tokenRepository.get())
        assertEquals(pushToken, tokenRepository.get())
        assertEquals(TokenType.HMS, tokenRepository.getLastTokenType())
    }

    @Test
    fun `should store FCM push token after SDK init with disabled automaticPushNotification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        initSdk(automaticPushNotification = false)
        val pushToken = "test-google-push-token"
        Exponea.trackPushToken(token = pushToken)
        assertNotNull(tokenRepository.get())
        assertEquals(pushToken, tokenRepository.get())
        assertEquals(TokenType.FCM, tokenRepository.getLastTokenType())
    }

    @Test
    fun `should store HMS push token after SDK init with disabled automaticPushNotification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        initSdk(automaticPushNotification = false)
        val pushToken = "test-google-push-token"
        Exponea.trackHmsPushToken(token = pushToken)
        assertNotNull(tokenRepository.get())
        assertEquals(pushToken, tokenRepository.get())
        assertEquals(TokenType.HMS, tokenRepository.getLastTokenType())
    }
}
