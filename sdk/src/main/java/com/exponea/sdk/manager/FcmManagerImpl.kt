package com.exponea.sdk.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.app.NotificationCompat
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.util.Logger
import java.io.IOException
import java.net.URL
import kotlin.concurrent.thread


class FcmManagerImpl(
        private val context: Context,
        private val configuration: ExponeaConfiguration
) : FcmManager {

    private lateinit var pendingIntent: PendingIntent
    private lateinit var pushReceiverIntent: Intent
    private val requestCode = 1
    private var smallIconRes = -1

    override fun showNotification(
            title: String,
            message: String,
            data: NotificationData?,
            id: Int,
            manager: NotificationManager,
            messageData: HashMap<String, String>
    ) {
        Logger.d(this, "showNotification")

        pushReceiverIntent = ExponeaPushReceiver.getClickIntent(context, id, data, messageData)
        pendingIntent = PendingIntent.getBroadcast(context, requestCode, pushReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // If push icon was not provided in the configuration, default one will be used
        smallIconRes = configuration.pushIcon ?: android.R.drawable.ic_dialog_info

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

        handlePayload(notification, messageData)
        manager.notify(id, notification.build())
    }

    private fun handlePayload(notification: NotificationCompat.Builder, messageData: HashMap<String, String>) {
        val notificationPayload = NotificationPayload(messageData)
        handlePayloadImage(notification, notificationPayload)
        handlePayloadSound(notification, notificationPayload)
        handlePayloadButtons(notification, notificationPayload)
        handlePayloadNotificationAction(notification, notificationPayload)
        handlePayloadAttributes(notificationPayload)
    }

    override fun createNotificationChannel(manager: NotificationManager) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = configuration.pushChannelName
            val description = configuration.pushChannelDescription
            val importance = configuration.pushNotificationImportance
            val channel = NotificationChannel(configuration.pushChannelId, name, importance)

            channel.description = description
            channel.setShowBadge(true)
            // Remove the default notification sound as it can be customized via payload and we
            // can't change it after setting it
            channel.setSound(null, null)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            manager.createNotificationChannel(channel)
        }
    }

    private fun handlePayloadImage(notification: NotificationCompat.Builder, messageData: NotificationPayload) {
        // Load the image in the payload and add as a big picture in the notification
        if (messageData.image != null) {
            val bigImageBitmap = getBitmapFromUrl(messageData.image)
            //verify if the image was successfully loaded
            if (bigImageBitmap != null) {
                notification.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bigImageBitmap))
            }
        }
    }

    private fun handlePayloadSound(notification: NotificationCompat.Builder, messageData: NotificationPayload) {
        // remove default notification sound
        notification.setSound(null)
        // set the uri for the default sound
        var soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // if the raw file exists, use it as custom sound
        if (messageData.sound != null && context.resources.getIdentifier(messageData.sound, "raw", context.packageName) != 0)
            soundUri = Uri.parse("""android.resource://${context.packageName}/raw/${messageData.sound}""")
        // Manually play the notification sound
        RingtoneManager.getRingtone(context, soundUri)?.also { it.play() }
    }

    private fun handlePayloadButtons(notification: NotificationCompat.Builder, messageData: NotificationPayload) {
        if (messageData.buttons != null) {
            //if we have a button payload, verify each button action
            messageData.buttons.forEach {
                val pi = generateActionPendingIntent(it.action, it.url)
                notification.addAction(0, it.title, pi)
            }
        }
    }

    private fun handlePayloadNotificationAction(notification: NotificationCompat.Builder, messageData: NotificationPayload) {
        //handle the notification body click action
        if (messageData.notificationAction != null) {
            with(messageData) {
                if (notificationAction?.url != null && notificationAction.action != null) {
                    var url = notificationAction.url
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "http://$url"
                    }
                    val pi = generateActionPendingIntent(notificationAction.action, url)
                    notification.setContentIntent(pi)
                }
            }
        }
    }

    private fun handlePayloadAttributes(messageData: NotificationPayload) {
        if (messageData.attributes != null) {
            if (Exponea.notificationDataCallback == null) {
                Exponea.component.pushNotificationRepository.setExtraData(messageData.attributes)
                return
            }
            Handler(Looper.getMainLooper()).post {
                Exponea.notificationDataCallback?.invoke(messageData.attributes)
            }
        }
    }

    private fun generateActionPendingIntent(action: NotificationPayload.Actions?, url: String? = null): PendingIntent? {
        val actionIntent = pushReceiverIntent
        actionIntent.putExtra(ExponeaPushReceiver.EXTRA_URL, url)

        return when (action) {
            NotificationPayload.Actions.APP -> pendingIntent
            NotificationPayload.Actions.BROWSER -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_URL_CLICKED
                PendingIntent.getBroadcast(context, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            NotificationPayload.Actions.DEEPLINK -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_DEEPLINK_CLICKED
                PendingIntent.getBroadcast(context, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            else -> pendingIntent
        }
    }

    private fun getBitmapFromUrl(url: String): Bitmap? {
        var bmp: Bitmap? = null
        thread {
            try {
                val input = URL(url).openStream()
                bmp = BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.join()
        return bmp
    }

}