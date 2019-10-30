package com.exponea.sdk.manager

import android.app.NotificationManager
import com.exponea.sdk.models.NotificationPayload
import com.google.firebase.messaging.RemoteMessage

internal interface FcmManager {
    fun trackFcmToken(token: String? = null)
    fun handleRemoteMessage(message: RemoteMessage?, manager: NotificationManager, showNotification: Boolean = true)
    fun showNotification(manager: NotificationManager, payload: NotificationPayload)
    fun createNotificationChannel(manager: NotificationManager)
}

