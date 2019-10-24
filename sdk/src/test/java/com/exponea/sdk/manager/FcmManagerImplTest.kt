package com.exponea.sdk.manager

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.FirebaseTokenRepositoryImpl
import com.google.firebase.messaging.RemoteMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


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
            FirebaseTokenRepositoryImpl(ExponeaPreferencesImpl(context))
        )
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun handleNullDataNotificationTest() {
        val data: NotificationData? = null
        manager.showNotification("", "", data, 0, notificationManager, hashMapOf())
    }

    @Test
    fun handleNullRemoteMessageNotificationTest() {
        val data: RemoteMessage? = null
        manager.handleRemoteMessage(data, notificationManager)
    }

}
