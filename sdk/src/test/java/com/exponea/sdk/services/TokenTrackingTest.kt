package com.exponea.sdk.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.util.TokenType
import io.mockk.every
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        Exponea.flushMode = FlushMode.MANUAL
    }

    @Test
    fun `should track fcm token when Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.handleNewToken(context, "mock token")
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf(
                    "google_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        assertNotNull(tokenRepository.get())
        assertEquals("mock token", tokenRepository.get())
        assertEquals(TokenType.FCM, tokenRepository.getLastTokenType())
        assertNotNull(tokenRepository.getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should track fcm token when Exponea Config is available`() {
        ExponeaConfigRepository.set(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.handleNewToken(context, "mock token")
        verify {
            anyConstructed<EventManagerImpl>().track(
                "campaign",
                any(),
                hashMapOf(
                    "google_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        assertNotNull(tokenRepository.get())
        assertEquals("mock token", tokenRepository.get())
        assertEquals(TokenType.FCM, tokenRepository.getLastTokenType())
        assertNotNull(tokenRepository.getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should store fcm token when Exponea Config is NOT available`() {
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        val pushToken = "mock token"
        Exponea.handleNewToken(context, pushToken)
        // verify that token is stored
        assertNotNull(tokenRepository.get())
        assertEquals(pushToken, tokenRepository.get())
        assertEquals(TokenType.FCM, tokenRepository.getLastTokenType())
        assertNull(tokenRepository.getLastTrackDateInMilliseconds())
        // but verify that tracking was not called
        verify(exactly = 0) {
            anyConstructed<EventManagerImpl>().track(
                "campaign",
                any(),
                any(),
                EventType.PUSH_TOKEN
            )
        }
    }

    @Test
    fun `should store HMS token when Exponea Config is NOT available`() {
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        val pushToken = "mock token"
        Exponea.handleNewHmsToken(context, pushToken)
        // verify that token is stored
        assertNotNull(tokenRepository.get())
        assertEquals(pushToken, tokenRepository.get())
        assertEquals(TokenType.HMS, tokenRepository.getLastTokenType())
        // but verify that tracking was not called
        verify(exactly = 0) {
            anyConstructed<EventManagerImpl>().track(
                "campaign",
                any(),
                any(),
                EventType.PUSH_TOKEN
            )
        }
    }

    @Test
    fun `should track fcm token after Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.handleNewToken(context, "mock token")
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf(
                    "google_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
    }

    /**
     * Purpose of this test is to re-track push token for fresh install of app.
     * Freshly installed app does not contains a stored SDK config
     * and developer may init SDK later than Application::onCreate.
     * Normally, Firebase will not update token on next app run and developer is not be aware of it,
     * so token will not be sent to BE without additional token-getter implementation
     */
    @Test
    fun `should post-track fcm token on first Exponea init`() {
        // this is required for RN and Flutter wrappers or native SDK delayed init (not Application::onCreate)
        // Firebase sends a new token, but no SDK config is stored
        Exponea.handleNewToken(context, "mock token")
        // And SDK is initialized later
        Exponea.init(context, ExponeaConfiguration(projectToken = "mock-token"))
        // should re-track a stored token
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf(
                    "google_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
    }

    /**
     * Purpose of this test is to re-track push token for fresh install of app.
     * Freshly installed app does not contains a stored SDK config
     * and developer may init SDK later than Application::onCreate.
     * Normally, HMS will not update token on next app run and developer is not be aware of it,
     * so token will not be sent to BE without additional token-getter implementation
     */
    @Test
    fun `should post-track HMS token on first Exponea init`() {
        // this is required for RN and Flutter wrappers or native SDK delayed init (not Application::onCreate)
        // HMS sends a new token, but no SDK config is stored
        Exponea.handleNewHmsToken(context, "mock token")
        // And SDK is initialized later
        Exponea.init(context, ExponeaConfiguration(projectToken = "mock-token"))
        // should re-track a stored token
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf(
                    "huawei_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should track hms token when Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.handleNewHmsToken(context, "mock token")
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf(
                    "huawei_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        assertNotNull(tokenRepository.get())
        assertEquals("mock token", tokenRepository.get())
        assertEquals(TokenType.HMS, tokenRepository.getLastTokenType())
        assertNotNull(tokenRepository.getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should track hms token when Exponea Config is available`() {
        ExponeaConfigRepository.set(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.handleNewHmsToken(context, "mock token")
        verify {
            anyConstructed<EventManagerImpl>().track(
                "campaign",
                any(),
                hashMapOf(
                    "huawei_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        assertNotNull(tokenRepository.get())
        assertEquals("mock token", tokenRepository.get())
        assertEquals(TokenType.HMS, tokenRepository.getLastTokenType())
        assertNotNull(tokenRepository.getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should track hms token after Exponea is initialized`() {
        Exponea.init(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.handleNewHmsToken(context, "mock token")
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
        verify {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                hashMapOf(
                    "huawei_push_notification_id" to "mock token"
                ),
                EventType.PUSH_TOKEN
            )
        }
        assertEquals("mock token", PushTokenRepositoryProvider.get(context).get())
        assertNotNull(PushTokenRepositoryProvider.get(context).getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should not track fcm token when Exponea was initialized but stopped`() {
        Exponea.init(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.isStopped = true
        Exponea.handleNewToken(context, "mock token")
        verify(exactly = 0) {
            Exponea.componentForTesting.eventManager.track(
                "campaign",
                any(),
                any(),
                EventType.PUSH_TOKEN
            )
        }
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        assertNull(tokenRepository.get())
        assertNull(tokenRepository.getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should not track fcm token when Exponea Config is available but stopped`() {
        ExponeaConfigRepository.set(context, ExponeaConfiguration(projectToken = "mock-token"))
        Exponea.isStopped = true
        Exponea.handleNewToken(context, "mock token")
        verify(exactly = 0) {
            anyConstructed<EventManagerImpl>().track(
                "campaign",
                any(),
                any(),
                EventType.PUSH_TOKEN
            )
        }
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        assertNull(tokenRepository.get())
        assertNull(tokenRepository.getLastTrackDateInMilliseconds())
    }

    @Test
    fun `should not store fcm token when Exponea Config is NOT available but stopped`() {
        val tokenRepository = PushTokenRepositoryProvider.get(context)
        val pushToken = "mock token"
        Exponea.isStopped = true
        Exponea.handleNewToken(context, pushToken)
        // verify that token is not stored
        assertNull(tokenRepository.get())
        assertNull(tokenRepository.getLastTrackDateInMilliseconds())
        // but verify that tracking was not called
        verify(exactly = 0) {
            anyConstructed<EventManagerImpl>().track(
                "campaign",
                any(),
                any(),
                EventType.PUSH_TOKEN
            )
        }
    }
}
