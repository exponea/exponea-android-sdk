package com.exponea.sdk.services

import android.app.NotificationManager
import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ExponeaFirebaseMessageService : FirebaseMessagingService() {
    private val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)

        if (!Exponea.isAutoPushNotification) {
            return
        }

        Logger.d(this, "Push Notification received.")

        val title = message?.data?.get("title") ?: ""

        val body = message?.data?.get("body") ?: ""

        val notificationId = message?.data?.get("notification_id")?.toInt() ?: 0

        // Configure the notification channel for push notifications on API 26+

        // This configuration runs only once.

        if (!Exponea.component.pushNotificationRepository.get()) {
            Exponea.component.fcmManager.createNotificationChannel(notificationManager)
            Exponea.component.pushNotificationRepository.set(true)
        }

        // Track the delivered push event to Exponea API.
        Exponea.component.pushManager.trackDeliveredPush()

        // Show push notification.
        Exponea.component.fcmManager.showNotification(
                title,
                body,
                notificationId,
                notificationManager
        )
    }
}