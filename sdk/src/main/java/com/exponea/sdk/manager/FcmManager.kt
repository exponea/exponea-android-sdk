package com.exponea.sdk.manager

import android.app.NotificationManager
import com.exponea.sdk.models.NotificationData
import com.google.firebase.messaging.RemoteMessage

interface FcmManager {
    fun showRemoteMessageNotification(message: RemoteMessage?, manager: NotificationManager)
    fun showNotification(title: String, message: String, data: NotificationData?, id: Int, manager: NotificationManager, messageData: HashMap<String, String>)
    fun createNotificationChannel(manager: NotificationManager)
}