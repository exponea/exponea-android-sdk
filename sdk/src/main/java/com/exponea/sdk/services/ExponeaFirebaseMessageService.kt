package com.exponea.sdk.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
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
            if (!areNotificationsBlockedForExponea()) {
                if (!Exponea.isAutoPushNotification) {
                    return@autoInitialize
                }
                Exponea.handleRemoteMessage(applicationContext, message, notificationManager)
            } else {
                Logger.i(this, "Notification delivery not handled," +
                        " notifications for the app are turned off in the settings")
            }
        }
    }

    internal fun areNotificationsBlockedForExponea(): Boolean {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val exponeaNotificationChannel =
                Exponea.pushChannelId?.let { notificationManager.getNotificationChannel(it) }
            return exponeaNotificationChannel?.isChannelBlocked(notificationManager) == true
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.isChannelBlocked(notificationManager: NotificationManagerCompat): Boolean {
        if (importance == NotificationManager.IMPORTANCE_NONE) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return notificationManager.getNotificationChannelGroup(group)?.isBlocked == true
        }
        return true
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
