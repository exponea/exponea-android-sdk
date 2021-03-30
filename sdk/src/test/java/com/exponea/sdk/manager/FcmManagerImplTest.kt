package com.exponea.sdk.manager

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.FirebaseTokenRepository
import com.exponea.sdk.repository.FirebaseTokenRepositoryImpl
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.shadows.ShadowResourcesWithAllResources
import com.exponea.sdk.shadows.ShadowRingtone
import com.exponea.sdk.testutil.data.NotificationTestPayloads
import com.exponea.sdk.testutil.data.NotificationTestPayloads.DEEPLINK_NOTIFICATION
import com.google.firebase.messaging.RemoteMessage
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
internal class FcmManagerImplTest {
    private lateinit var context: Context
    private lateinit var manager: FcmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var eventManager: EventManager
    private lateinit var firebaseTokenRepository: FirebaseTokenRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        eventManager = mockkClass(EventManagerImpl::class)
        every { eventManager.track(any(), any(), any(), any()) } just Runs
        firebaseTokenRepository = spyk(FirebaseTokenRepositoryImpl(ExponeaPreferencesImpl(context)))
        manager = FcmManagerImpl(
            context,
            ExponeaConfiguration(),
            eventManager,
            firebaseTokenRepository,
            PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context))
        )
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    }

    @After
    fun tearDown() {
        firebaseTokenRepository.clear()
    }

    @Test
    fun `should not crash showing notification with empty payload`() {
        manager.showNotification(notificationManager, NotificationPayload(HashMap()))
    }

    @Test
    fun `should handle null notification`() {
        manager.handleRemoteMessage(null, notificationManager)
    }

    @Test
    fun `should generate correct intents for 2 notifications`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs

        val notification1Payload = HashMap(DEEPLINK_NOTIFICATION)
        notification1Payload["url"] = "app://mock-url-1"
        manager.showNotification(notificationManager, NotificationPayload(notification1Payload))
        val intent1 = shadowOf(notificationSlot.captured.contentIntent).savedIntent

        val notification2Payload = HashMap(DEEPLINK_NOTIFICATION)
        notification2Payload["url"] = "app://mock-url-2"
        manager.showNotification(notificationManager, NotificationPayload(notification2Payload))
        val intent2 = shadowOf(notificationSlot.captured.contentIntent).savedIntent

        assertEquals(
            NotificationAction("notification", null, "app://mock-url-1"),
            intent1.extras?.get(ExponeaPushReceiver.EXTRA_ACTION_INFO)
        )

        assertEquals(
            NotificationAction("notification", null, "app://mock-url-2"),
            intent2.extras?.get(ExponeaPushReceiver.EXTRA_ACTION_INFO)
        )
    }

    @Test
    fun `should not show subsequent notifications with same id`() {
        val notification = RemoteMessage.Builder("1")
            .setData(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
            .build()
        manager.handleRemoteMessage(notification, notificationManager, true)
        manager.handleRemoteMessage(notification, notificationManager, true)
        verify(exactly = 1) { notificationManager.notify(any(), any()) }
        notification.data["notification_id"] = "2"
        manager.handleRemoteMessage(notification, notificationManager, true)
        verify(exactly = 2) { notificationManager.notify(any(), any()) }
    }

    @Test
    fun `should track token in ON_TOKEN_CHANGE mode`() {
        manager.trackFcmToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE)
        manager.trackFcmToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE)
        manager.trackFcmToken("other_token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE)
        verify(exactly = 1) { firebaseTokenRepository.set("token", any()) }
        verify(exactly = 1) { firebaseTokenRepository.set("other_token", any()) }
    }

    @Test
    fun `should track token in DAILY mode`() {
        // there is a bug in robolectric, we have to set time https://github.com/robolectric/robolectric/issues/3912
        ShadowSystemClock.setNanoTime(System.currentTimeMillis() * 1000 * 1000)
        manager.trackFcmToken("token", ExponeaConfiguration.TokenFrequency.DAILY)
        verify(exactly = 1) { firebaseTokenRepository.set("token", any()) }
        manager.trackFcmToken("other_token", ExponeaConfiguration.TokenFrequency.DAILY)
        verify(exactly = 0) { firebaseTokenRepository.set("other_token", any()) }
        firebaseTokenRepository.set("token", System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        manager.trackFcmToken("other_token", ExponeaConfiguration.TokenFrequency.DAILY)
        verify(exactly = 1) { firebaseTokenRepository.set("other_token", any()) }
    }

    @Test
    fun `should track token in EVERY_LAUNCH mode`() {
        manager.trackFcmToken("token", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH)
        manager.trackFcmToken("token", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH)
        manager.trackFcmToken("other_token", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH)
        verify(exactly = 2) { firebaseTokenRepository.set("token", any()) }
        verify(exactly = 1) { firebaseTokenRepository.set("other_token", any()) }
    }

    @Test
    @Config(shadows = [ShadowRingtone::class])
    fun `should play default sound`() {
        val notification = RemoteMessage.Builder("1")
            .setData(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
            .build()
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertEquals("content://settings/system/notification_sound", ShadowRingtone.lastRingtone?.withUri.toString())
    }

    @Test
    @Config(shadows = [ShadowRingtone::class])
    fun `should not play sound in DnD mode`() {
        val notification = RemoteMessage.Builder("1")
            .setData(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
            .build()
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_PRIORITY
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should not play sound when channel doesn't exist`() {
        val notification = RemoteMessage.Builder("1")
            .setData(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
            .build()
        every { notificationManager.getNotificationChannel(any()) } returns null
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(shadows = [ShadowRingtone::class, ShadowResourcesWithAllResources::class])
    fun `should play sound from notification payload`() {
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        payload["sound"] = "mock-sound.mp3"
        val notification = RemoteMessage.Builder("1").setData(payload).build()
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertEquals(
            "android.resource://com.exponea.sdk.test/raw/mock-sound.mp3",
            ShadowRingtone.lastRingtone?.withUri.toString()
        )
    }

    @Test
    fun `should show payload image`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        payload["image"] = "https://raw.githubusercontent.com/exponea/" +
            "exponea-android-sdk/develop/Documentation/logo_yellow.png"
        val notification = RemoteMessage.Builder("1").setData(payload).build()
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertNotNull(shadowOf(notificationSlot.captured).bigPicture)
    }

    @Test
    @Config(shadows = [ShadowResourcesWithAllResources::class])
    fun `should use push icon from configuration`() {
        manager = FcmManagerImpl(
            context,
            ExponeaConfiguration(pushIcon = 123),
            eventManager,
            firebaseTokenRepository,
            PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context))
        )
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        val notification = RemoteMessage.Builder("1")
            .setData(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
            .build()
        manager.handleRemoteMessage(notification, notificationManager, true)
        @Suppress("DEPRECATION")
        assertEquals(123, notificationSlot.captured.icon)
    }
}
