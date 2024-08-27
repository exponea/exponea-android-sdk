package com.exponea.sdk.manager

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.ExponeaExtras
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.receiver.NotificationsPermissionReceiver
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepository
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.shadows.OnlyDefaultShadowRingtone
import com.exponea.sdk.shadows.ShadowResourcesWithAllResources
import com.exponea.sdk.shadows.ShadowRingtone
import com.exponea.sdk.testutil.data.NotificationTestPayloads
import com.exponea.sdk.testutil.data.NotificationTestPayloads.DEEPLINK_NOTIFICATION
import com.exponea.sdk.testutil.resetVerifyMockkCount
import com.exponea.sdk.util.TokenType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.LooperMode.Mode.LEGACY
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
internal class FcmManagerImplTest {
    private lateinit var context: Context
    private lateinit var manager: FcmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var eventManager: EventManager
    private lateinit var pushTokenRepository: PushTokenRepository
    private lateinit var trackingConsentManager: TrackingConsentManager

    @Before
    fun setUp() {
        ShadowRingtone.lastRingtone = null
        mockkObject(NotificationsPermissionReceiver)
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns true
        context = ApplicationProvider.getApplicationContext<Context>()
        eventManager = mockkClass(EventManagerImpl::class)
        every { eventManager.track(any(), any(), any(), any()) } just Runs
        pushTokenRepository = spyk(PushTokenRepositoryProvider.get(context))
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        trackingConsentManager = mockkClass(TrackingConsentManagerImpl::class)
        every {
            trackingConsentManager.trackDeliveredPush(any(), any(), any(), any(), any())
        } just Runs
        manager = createFcmManager()
    }

    private fun createFcmManager(
        notifImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        requirePushAuthorization: Boolean = Constants.PushNotif.defaultPushAuthorizationRequired
    ) = FcmManagerImpl(
        context,
        ExponeaConfiguration(
            pushNotificationImportance = notifImportance,
            requirePushAuthorization = requirePushAuthorization
        ),
        eventManager,
        pushTokenRepository,
        PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context)),
        trackingConsentManager
    )

    @After
    fun tearDown() {
        pushTokenRepository.clear()
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
            intent1.extras?.get(ExponeaExtras.EXTRA_ACTION_INFO)
        )

        assertEquals(
            NotificationAction("notification", null, "app://mock-url-2"),
            intent2.extras?.get(ExponeaExtras.EXTRA_ACTION_INFO)
        )
    }

    @Test
    fun `should not show subsequent notifications with same id`() {
        val notification = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        manager.handleRemoteMessage(notification, notificationManager, true)
        manager.handleRemoteMessage(notification, notificationManager, true)
        verify(exactly = 1) { notificationManager.notify(any(), any()) }
        notification["notification_id"] = "2"
        manager.handleRemoteMessage(notification, notificationManager, true)
        verify(exactly = 2) { notificationManager.notify(any(), any()) }
    }

    @Test
    fun `should track token in ON_TOKEN_CHANGE mode`() {
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        manager.trackToken("other_token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("token", any(), TokenType.FCM, true) }
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("other_token", any(), TokenType.FCM, true) }
    }

    @Test
    fun `should track token in ON_TOKEN_CHANGE mode after permission granted`() {
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns false
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("token", any(), TokenType.FCM, false) }
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns true
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("token", any(), TokenType.FCM, true) }
    }

    @Test
    fun `should track token in ON_TOKEN_CHANGE mode after permission denied`() {
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns true
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns false
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("token", any(), TokenType.FCM, true) }
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("token", any(), TokenType.FCM, false) }
    }

    @Test
    @LooperMode(LEGACY)
    fun `should track token in DAILY mode`() {
        // there is a bug in robolectric, we have to set time https://github.com/robolectric/robolectric/issues/3912
        ShadowSystemClock.setNanoTime(System.currentTimeMillis() * 1000 * 1000)
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.DAILY, TokenType.FCM)
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("token", any(), TokenType.FCM, true) }
        manager.trackToken("other_token", ExponeaConfiguration.TokenFrequency.DAILY, TokenType.FCM)
        verify(exactly = 0) { pushTokenRepository.setTrackedToken("other_token", any(), TokenType.FCM, true) }
        pushTokenRepository.setTrackedToken(
            "token",
            System.currentTimeMillis() - 24 * 60 * 60 * 1000,
            TokenType.FCM,
            true
        )
        manager.trackToken("other_token", ExponeaConfiguration.TokenFrequency.DAILY, TokenType.FCM)
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("other_token", any(), TokenType.FCM, true) }
    }

    @Test
    fun `should track token in EVERY_LAUNCH mode`() {
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, TokenType.FCM)
        manager.trackToken("token", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, TokenType.FCM)
        manager.trackToken("other_token", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, TokenType.FCM)
        verify(exactly = 2) { pushTokenRepository.setTrackedToken("token", any(), TokenType.FCM, true) }
        verify(exactly = 1) { pushTokenRepository.setTrackedToken("other_token", any(), TokenType.FCM, true) }
    }

    @Test
    fun `should track empty token in ON_TOKEN_CHANGE if permission denied but required (forcibly)`() {
        val pushToken = "mock-push-token"
        val permissionGranted = false
        val pushTokenPropsSlot = slot<HashMap<String, Any>>()
        manager = createFcmManager(requirePushAuthorization = true)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken, System.currentTimeMillis(), TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken, ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        // has to store exactly-same token again
        verify(exactly = 1) { pushTokenRepository.setTrackedToken(pushToken, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 1) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            capture(pushTokenPropsSlot),
            EventType.PUSH_TOKEN,
            any()
        ) }
        assertEquals("", pushTokenPropsSlot.captured[TokenType.FCM.apiProperty])
    }

    @Test
    fun `should track empty token in DAILY if permission denied but required (forcibly)`() {
        val pushToken = "mock-push-token"
        val permissionGranted = false
        val pushTokenPropsSlot = slot<HashMap<String, Any>>()
        manager = createFcmManager(requirePushAuthorization = true)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken, System.currentTimeMillis(), TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken, ExponeaConfiguration.TokenFrequency.DAILY, TokenType.FCM)
        // has to store exactly-same token again
        verify(exactly = 1) { pushTokenRepository.setTrackedToken(pushToken, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 1) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            capture(pushTokenPropsSlot),
            EventType.PUSH_TOKEN,
            any()
        ) }
        assertEquals("", pushTokenPropsSlot.captured[TokenType.FCM.apiProperty])
    }

    @Test
    fun `should track empty token in EVERY_LAUNCH if permission denied but required (forcibly)`() {
        val pushToken = "mock-push-token"
        val permissionGranted = false
        val pushTokenPropsSlot = slot<HashMap<String, Any>>()
        manager = createFcmManager(requirePushAuthorization = true)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken, System.currentTimeMillis(), TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken, ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, TokenType.FCM)
        // has to store exactly-same token again
        verify(exactly = 1) { pushTokenRepository.setTrackedToken(pushToken, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 1) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            capture(pushTokenPropsSlot),
            EventType.PUSH_TOKEN,
            any()
        ) }
        assertEquals("", pushTokenPropsSlot.captured[TokenType.FCM.apiProperty])
    }

    @Test
    fun `should track full token in EVERY_LAUNCH if permission denied but not required`() {
        val pushToken = "mock-push-token"
        val permissionGranted = false
        val pushTokenPropsSlot = slot<HashMap<String, Any>>()
        manager = createFcmManager(requirePushAuthorization = false)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken, System.currentTimeMillis(), TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken, ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, TokenType.FCM)
        // has to store exactly-same token again
        verify(exactly = 1) { pushTokenRepository.setTrackedToken(pushToken, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 1) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            capture(pushTokenPropsSlot),
            EventType.PUSH_TOKEN,
            any()
        ) }
        assertEquals(pushToken, pushTokenPropsSlot.captured[TokenType.FCM.apiProperty])
    }

    @Test
    fun `should not track same token in ON_TOKEN_CHANGE if permission denied but not required`() {
        val pushToken = "mock-push-token"
        val permissionGranted = false
        manager = createFcmManager(requirePushAuthorization = false)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken, System.currentTimeMillis(), TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken, ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        // has to store exactly-same token again
        verify(exactly = 0) { pushTokenRepository.setTrackedToken(pushToken, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 0) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            any(),
            EventType.PUSH_TOKEN,
            any()
        ) }
    }

    @Test
    fun `should track new full token in ON_TOKEN_CHANGE if permission denied but not required`() {
        val pushToken1 = "mock-push-token-1"
        val pushToken2 = "mock-push-token-2"
        val permissionGranted = false
        val pushTokenPropsSlot = slot<HashMap<String, Any>>()
        manager = createFcmManager(requirePushAuthorization = false)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken1, System.currentTimeMillis(), TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken2, ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE, TokenType.FCM)
        // has to store new token
        verify(exactly = 1) { pushTokenRepository.setTrackedToken(pushToken2, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 1) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            capture(pushTokenPropsSlot),
            EventType.PUSH_TOKEN,
            any()
        ) }
        assertEquals(pushToken2, pushTokenPropsSlot.captured[TokenType.FCM.apiProperty])
    }

    @Test
    @LooperMode(LEGACY)
    fun `should not track fresh token in DAILY if permission denied but not required`() {
        val pushToken = "mock-push-token"
        val permissionGranted = false
        manager = createFcmManager(requirePushAuthorization = false)
        // there is a bug in robolectric, we have to set time https://github.com/robolectric/robolectric/issues/3912
        ShadowSystemClock.setNanoTime(System.currentTimeMillis() * 1000 * 1000)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken, System.currentTimeMillis(), TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken, ExponeaConfiguration.TokenFrequency.DAILY, TokenType.FCM)
        // has to store exactly-same token again
        verify(exactly = 0) { pushTokenRepository.setTrackedToken(pushToken, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 0) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            any(),
            EventType.PUSH_TOKEN,
            any()
        ) }
    }

    @Test
    fun `should track new full token in DAILY if permission denied but not required`() {
        val pushToken = "mock-push-token"
        val permissionGranted = false
        val pushTokenPropsSlot = slot<HashMap<String, Any>>()
        manager = createFcmManager(requirePushAuthorization = false)
        // simulate previous token track
        pushTokenRepository.setTrackedToken(pushToken, 0, TokenType.FCM, permissionGranted)
        pushTokenRepository.resetVerifyMockkCount()
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns permissionGranted
        // track same token again
        manager.trackToken(pushToken, ExponeaConfiguration.TokenFrequency.DAILY, TokenType.FCM)
        // has to store exactly-same token again
        verify(exactly = 1) { pushTokenRepository.setTrackedToken(pushToken, any(), TokenType.FCM, permissionGranted) }
        verify(exactly = 1) { eventManager.track(
            Constants.EventTypes.push,
            any(),
            capture(pushTokenPropsSlot),
            EventType.PUSH_TOKEN,
            any()
        ) }
        assertEquals(pushToken, pushTokenPropsSlot.captured[TokenType.FCM.apiProperty])
    }

    @Test
    @Config(shadows = [ShadowRingtone::class], sdk = [Build.VERSION_CODES.P])
    fun `should play default sound`() {
        val notification = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertEquals("content://settings/system/notification_sound", ShadowRingtone.lastRingtone?.withUri.toString())
    }

    @Test
    @Config(
        shadows = [OnlyDefaultShadowRingtone::class, ShadowResourcesWithAllResources::class],
        sdk = [Build.VERSION_CODES.P]
    )
    fun `should play default sound as fallback`() {
        val notification = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        notification["sound"] = "beep"
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertEquals("content://settings/system/notification_sound", ShadowRingtone.lastRingtone?.withUri.toString())
    }

    @Test
    @Config(shadows = [ShadowRingtone::class])
    fun `should not play sound in DnD mode`() {
        val notification = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        every { notificationManager.currentInterruptionFilter } returns NotificationManager.INTERRUPTION_FILTER_PRIORITY
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should not play sound when channel doesn't exist`() {
        val notification = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        every { notificationManager.getNotificationChannel(any()) } returns null
        manager.handleRemoteMessage(notification, notificationManager, true)
        assertNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should not play sound when channel importance is IMPORTANCE_NONE`() {
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_NONE)
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertFalse(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should not play sound when channel importance is IMPORTANCE_MIN`() {
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_MIN)
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertFalse(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should not play sound when channel importance is IMPORTANCE_LOW`() {
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_LOW)
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertFalse(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should play sound when channel importance is IMPORTANCE_DEFAULT`() {
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_DEFAULT)
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertNotNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should play sound when channel importance is IMPORTANCE_HIGH`() {
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_HIGH)
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertNotNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], shadows = [ShadowRingtone::class])
    fun `should play sound when channel importance is IMPORTANCE_MAX`() {
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_MAX)
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertNotNull(ShadowRingtone.lastRingtone)
    }

    @Test
    @Config(shadows = [ShadowRingtone::class, ShadowResourcesWithAllResources::class], sdk = [Build.VERSION_CODES.P])
    fun `should play sound from notification payload for sound name with extension`() {
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        payload["sound"] = "mock-sound.mp3"
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertEquals(
            "android.resource://com.exponea.sdk.test/raw/mock-sound",
            ShadowRingtone.lastRingtone?.withUri.toString()
        )
    }

    @Test
    @Config(shadows = [ShadowRingtone::class, ShadowResourcesWithAllResources::class], sdk = [Build.VERSION_CODES.P])
    fun `should play sound from notification payload for sound name without extension`() {
        val payload = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        payload["sound"] = "mock-sound"
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(ShadowRingtone.lastRingtone?.wasPlayed ?: false)
        assertEquals(
            "android.resource://com.exponea.sdk.test/raw/mock-sound",
            ShadowRingtone.lastRingtone?.withUri.toString()
        )
    }

    @Test
    fun `should not show notification when channel importance is IMPORTANCE_NONE`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_NONE)
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertFalse(notificationSlot.isCaptured)
    }

    @Test
    fun `should show notification when channel importance is IMPORTANCE_MIN`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_MIN)
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(notificationSlot.isCaptured)
    }

    @Test
    fun `should show notification when channel importance is IMPORTANCE_LOW`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_LOW)
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(notificationSlot.isCaptured)
    }

    @Test
    fun `should show notification when channel importance is IMPORTANCE_DEFAULT`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_DEFAULT)
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(notificationSlot.isCaptured)
    }

    @Test
    fun `should show notification when channel importance is IMPORTANCE_HIGH`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_HIGH)
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(notificationSlot.isCaptured)
    }

    @Test
    fun `should show notification when channel importance is IMPORTANCE_MAX`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        manager = createFcmManager(notifImportance = NotificationManager.IMPORTANCE_MAX)
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertTrue(notificationSlot.isCaptured)
    }

    @Test
    fun `should show payload image`() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        val payload = HashMap(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
        payload["image"] = "https://www.bloomreach.com/wp-content/uploads/2024/04/bloomreach.svg"
        manager.handleRemoteMessage(payload, notificationManager, true)
        assertNotNull(shadowOf(notificationSlot.captured).bigPicture)
    }

    @Test
    @Config(shadows = [ShadowResourcesWithAllResources::class])
    fun `should use push icon from configuration`() {
        manager = FcmManagerImpl(
            context,
            ExponeaConfiguration(pushIcon = 123),
            eventManager,
            pushTokenRepository,
            PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context)),
            trackingConsentManager
        )
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        manager.handleRemoteMessage(NotificationTestPayloads.PRODUCTION_NOTIFICATION, notificationManager, true)
        @Suppress("DEPRECATION")
        assertEquals(123, notificationSlot.captured.icon)
    }
}
