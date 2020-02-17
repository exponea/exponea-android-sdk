package com.exponea.sdk.services

import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import com.exponea.sdk.Exponea
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.toDate
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal class ExponeaFirebaseMessageService : FirebaseMessagingService() {

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        runCatching {
            onMessageReceivedUnsafe(message)
        }.logOnException()
    }

    private fun onMessageReceivedUnsafe(message: RemoteMessage) {
        Logger.d(this, "Push Notification received at ${currentTimeSeconds().toDate()}.")

        if (!Exponea.isInitialized) {
            val config = ExponeaConfigRepository.get(applicationContext)
            if (config != null) {
                Exponea.init(applicationContext, config)
            }
        }

        if (!Exponea.isAutoPushNotification) {
            return
        }

        Exponea.handleRemoteMessage(message, notificationManager)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        runCatching {
            onNewTokenUnsafe(token)
        }.logOnException()
    }

    private fun onNewTokenUnsafe(token: String) {
        if (!Exponea.isInitialized) {
            val config = ExponeaConfigRepository.get(applicationContext) ?: return
            Looper.prepare()
            Exponea.init(applicationContext, config)
        }
        if (!Exponea.isAutoPushNotification) {
            return
        }
        Logger.d(this, "Firebase Token Refreshed")
        Exponea.component.fcmManager.trackFcmToken(token)
    }
}
