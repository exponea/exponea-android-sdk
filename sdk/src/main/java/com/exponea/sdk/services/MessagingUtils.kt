package com.exponea.sdk.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.exponea.sdk.Exponea

internal class MessagingUtils {

    companion object {
        internal fun areNotificationsBlockedForTheApp(context: Context): Boolean {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val exponeaNotificationChannel =
                    Exponea.pushChannelId.let { notificationManager.getNotificationChannel(it) }
                return exponeaNotificationChannel?.isChannelBlocked(notificationManager) == true
            }
            return false
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun NotificationChannel.isChannelBlocked(notificationManager: NotificationManagerCompat): Boolean {
            if (importance == NotificationManager.IMPORTANCE_NONE) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return notificationManager.getNotificationChannelGroup(group)?.isBlocked == true
            }
            return false
        }

        internal fun getPendingIntentFlags(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }
    }
}
