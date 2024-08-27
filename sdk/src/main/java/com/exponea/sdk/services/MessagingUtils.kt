package com.exponea.sdk.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.exponea.sdk.models.NotificationChannelImportance
import com.exponea.sdk.util.Logger

internal class MessagingUtils {

    companion object {
        internal fun areNotificationsBlockedForTheApp(
            context: Context,
            notificationManager: NotificationManager,
            pushChannelId: String
        ): Boolean {
            val notificationsEnabledForAppGlobally: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
            if (!notificationsEnabledForAppGlobally) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val exponeaNotificationChannel = getNotificationChannel(context, notificationManager, pushChannelId)
                if (exponeaNotificationChannel == null) {
                    Logger.e(
                        this,
                        "Notification channel needs to be created. Push notifications are blocked until then"
                    )
                    return true
                }
                return exponeaNotificationChannel.isChannelBlocked(notificationManager)
            }
            return false
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun NotificationChannel.isChannelBlocked(notificationManager: NotificationManager): Boolean {
            if (importance == NotificationManager.IMPORTANCE_NONE) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return group?.let {
                    notificationManager.getNotificationChannelGroup(it)?.isBlocked
                } == true
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

        fun doesChannelExists(context: Context, notificationManager: NotificationManager, channelId: String): Boolean {
            return getNotificationChannel(context, notificationManager, channelId) != null
        }

        fun getNotificationChannel(
            context: Context,
            notificationManager: NotificationManager,
            channelId: String
        ): NotificationChannel? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.getNotificationChannel(channelId)
            } else {
                NotificationManagerCompat.from(context).getNotificationChannel(channelId)
            }
        }

        fun getNotificationChannelImportance(
            context: Context,
            notificationManager: NotificationManager,
            channelId: String
        ): NotificationChannelImportance {
            val notificationChannel = getNotificationChannel(context, notificationManager, channelId)
            if (notificationChannel == null) {
                return NotificationChannelImportance.UNKNOWN
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return NotificationChannelImportance.UNSUPPORTED
            }
            when (notificationChannel.importance) {
                NotificationManager.IMPORTANCE_UNSPECIFIED -> return NotificationChannelImportance.UNSPECIFIED
                NotificationManager.IMPORTANCE_NONE -> return NotificationChannelImportance.NONE
                NotificationManager.IMPORTANCE_MIN -> return NotificationChannelImportance.MIN
                NotificationManager.IMPORTANCE_LOW -> return NotificationChannelImportance.LOW
                NotificationManager.IMPORTANCE_DEFAULT -> return NotificationChannelImportance.DEFAULT
                NotificationManager.IMPORTANCE_HIGH -> return NotificationChannelImportance.HIGH
                NotificationManager.IMPORTANCE_MAX -> return NotificationChannelImportance.MAX
                else -> return NotificationChannelImportance.UNKNOWN
            }
        }
    }
}
