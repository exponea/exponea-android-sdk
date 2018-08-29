package com.exponea.sdk.services

import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder

class ExponeaFirebaseMessageService : FirebaseMessagingService() {
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)
        Logger.d(this, "Push Notification received.")

        if (!Exponea.isInitialized) {
            val config = ExponeaConfigRepository.get(applicationContext)
            if (config != null) {
                Looper.prepare()
                Exponea.init(applicationContext, config)
            }
        }

        if (!Exponea.isAutoPushNotification) {
            return
        }


        val title = message?.data?.get("title") ?: ""

        val body = message?.data?.get("message") ?: ""

        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        val dataString = message?.data?.get("data")
        val data = gson.fromJson(dataString, NotificationData::class.java)
        val notificationId = message?.data?.get("notification_id")?.toInt() ?: 0
        // Configure the notification channel for push notifications on API 26+

        // This configuration runs only once.

        if (!Exponea.component.pushNotificationRepository.get()) {
            Exponea.component.fcmManager.createNotificationChannel(notificationManager)
            Exponea.component.pushNotificationRepository.set(true)
        }

        // Track the delivered push event to Exponea API.
        Exponea.component.pushManager.trackDeliveredPush(
                data = data
        )

        //Create a map with all the data of the remote message, removing the data already processed
        val messageData = message?.data?.apply {
            remove("title")
            remove("message")
            remove("data")
            remove("notification_id")
        }?.run {
            HashMap(this)
        } ?: HashMap()

        // Show push notification.
        Exponea.component.fcmManager.showNotification(
                title,
                body,
                data,
                notificationId,
                notificationManager,
                messageData
        )
    }
}