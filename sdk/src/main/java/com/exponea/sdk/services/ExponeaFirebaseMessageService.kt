package com.exponea.sdk.services

import android.app.NotificationManager
import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.repository.FirebaseTokenRepositoryProvider
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
        Exponea.autoInitialize(applicationContext) {
            if (!Exponea.isAutoPushNotification) {
                return@autoInitialize
            }
            Exponea.handleRemoteMessage(applicationContext, message, notificationManager)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        runCatching {
            onNewTokenUnsafe(token)
        }.logOnException()
    }

    private fun onNewTokenUnsafe(token: String) {
        Logger.d(this, "Received push notification token")
        Exponea.autoInitialize(applicationContext,
            {
                Logger.d(this, "Exponea cannot be auto-initialized, token will be tracked once Exponea is initialized")
                FirebaseTokenRepositoryProvider.get(applicationContext).set(token, 0)
            },
            {
                if (!Exponea.isAutoPushNotification) {
                    return@autoInitialize
                }
                Logger.d(this, "Firebase Token Refreshed")
                Exponea.trackPushToken(token, Exponea.tokenTrackFrequency)
            }
        )
    }
}
