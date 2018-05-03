package com.exponea.sdk.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.exponea.sdk.models.ExponeaConfiguration
import android.app.NotificationChannel
import android.os.Build

class FcmManagerImpl(
        private val context: Context,
        private val configuration: ExponeaConfiguration
) : FcmManager {

    private val REQUEST_CODE = 1

    override fun showNotification(
            title: String,
            message: String,
            id: Int,
            manager: NotificationManager
    ) {
        val i = Intent(context, PushManager::class.java)

        i.addCategory(Intent.CATEGORY_HOME)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = PendingIntent.getActivity(
                context, REQUEST_CODE,
                i, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context)
                .setContentText(message)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)

        configuration.pushIcon?.let {
            notification
                    .setSmallIcon(it)
        }

        manager.notify(id, notification.build())
    }

    override fun createNotificationChannel(
            manager: NotificationManager
    ) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = configuration.pushChannelName
            val description = configuration.pushChannelDescription
            val importance = configuration.pushNotificationImportance
            val channel = NotificationChannel(configuration.pushChannelId, name, importance)

            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            manager.createNotificationChannel(channel)
        }
    }
}