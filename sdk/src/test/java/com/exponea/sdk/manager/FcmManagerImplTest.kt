package com.exponea.sdk.manager

import android.app.NotificationManager
import android.content.Context

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationData

import org.junit.Before
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class FcmManagerImplTest {

    private lateinit var manager: FcmManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        manager = FcmManagerImpl(RuntimeEnvironment.application.applicationContext, ExponeaConfiguration())
        notificationManager = RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun handleNullDataNotificationTest() {
        val data: NotificationData? = null
        manager.showNotification("", "", data, 0, notificationManager, hashMapOf())
    }

}