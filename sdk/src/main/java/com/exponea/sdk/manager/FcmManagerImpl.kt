package com.exponea.sdk.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.util.Logger

class FcmManagerImpl(
    private val context: Context,
    private val configuration: ExponeaConfiguration
) : FcmManager {

    private val requestCode = 1

    override fun showNotification(
        title: String,
        message: String,
        data: NotificationData?,
        id: Int,
        manager: NotificationManager,
        messageData: HashMap<String, String>
    ) {
        Logger.d(this, "showNotification")

        val i = ExponeaPushReceiver.getClickIntent(context, id, data, messageData)

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode,
            i, PendingIntent.FLAG_UPDATE_CURRENT
        )

        // If push icon was not provided in the configuration, default one will be used
        var smallIconRes = configuration.pushIcon ?: android.R.drawable.ic_dialog_info

        // Icon id was provided but was invalid
        try {
            context.resources.getResourceName(smallIconRes)
        } catch (exception: Resources.NotFoundException) {
            Logger.e(this, "Invalid icon resource: $smallIconRes")
            smallIconRes = android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(context, configuration.pushChannelId)
            .setContentText(message)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setChannelId(configuration.pushChannelId)
            .setSmallIcon(smallIconRes)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

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