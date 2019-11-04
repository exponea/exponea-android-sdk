package com.exponea.sdk.manager

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.NotificationTestPayloads.DEEPLINK_NOTIFICATION
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.FirebaseTokenRepositoryImpl
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.services.ExponeaPushReceiver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.spyk
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
internal class FcmManagerImplTest {

    private lateinit var manager: FcmManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager = FcmManagerImpl(
            context,
            ExponeaConfiguration(),
            FirebaseTokenRepositoryImpl(ExponeaPreferencesImpl(context)),
            PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context))
        )
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
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
        val intent1 = Shadows.shadowOf(notificationSlot.captured.contentIntent).savedIntent

        val notification2Payload = HashMap(DEEPLINK_NOTIFICATION)
        notification2Payload["url"] = "app://mock-url-2"
        manager.showNotification(notificationManager, NotificationPayload(notification2Payload))
        val intent2 = Shadows.shadowOf(notificationSlot.captured.contentIntent).savedIntent

        assertEquals(
            NotificationAction("notification", null, "app://mock-url-1"),
            intent1.extras.get(ExponeaPushReceiver.EXTRA_ACTION_INFO)
        )

        assertEquals(
            NotificationAction("notification", null, "app://mock-url-2"),
            intent2.extras.get(ExponeaPushReceiver.EXTRA_ACTION_INFO)
        )
    }
}
