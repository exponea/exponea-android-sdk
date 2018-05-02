package com.exponea.sdk.services

import android.app.NotificationManager
import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ExponeaFirebaseMessageService: FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)

        Logger.d(this, "Push Notification received.")

        val title = message?.data?.get("title")?.also {
            it
        } ?: run {
            ""
        }

        val body = message?.data?.get("body")?.also {
            it
        } ?: run {
            ""
        }

        val notificationId = message?.data?.get("notification_id")?.toInt().also {
            it
        } ?: run {
            0
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Exponea.component.fcmManager.showNotification(title, body, notificationId, manager)
    }
}