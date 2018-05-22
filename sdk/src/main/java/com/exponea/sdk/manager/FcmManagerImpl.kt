package com.exponea.sdk.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.Logger

class FcmManagerImpl(
        private val context: Context,
        private val configuration: ExponeaConfiguration
) : FcmManager {

    private val requestCode = 1

    override fun showNotification(
            title: String,
            message: String,
            id: Int,
            manager: NotificationManager
    ) {
        Logger.d(this, "showNotification")

        val i = Intent(context, PushManager::class.java)

        i.addCategory(Intent.CATEGORY_HOME)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = PendingIntent.getActivity(
                context, requestCode,
                i, PendingIntent.FLAG_UPDATE_CURRENT
        )

        // TODO if small icon is invalid the app will crash so this needs to be handled someway
        val smallIconRes = configuration.pushIcon

        if (smallIconRes == null) {
            Logger.d(this, "Invalid Icon Res: $smallIconRes")
            return
        }

        val notification = NotificationCompat.Builder(context, configuration.pushChannelName)
                .setContentText(message)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setSmallIcon(smallIconRes)

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