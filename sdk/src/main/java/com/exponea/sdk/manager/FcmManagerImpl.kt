package com.exponea.sdk.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson
import org.json.JSONArray
import java.io.IOException
import java.net.URL
import kotlin.concurrent.thread


class FcmManagerImpl(
        private val context: Context,
        private val configuration: ExponeaConfiguration
) : FcmManager {

    private val requestCode = 1
    private var pendingIntent: PendingIntent? = null
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

        val i = ExponeaPushReceiver.getClickIntent(context, id, data, messageData)

        pendingIntent = PendingIntent.getBroadcast(
                context, requestCode,
                i, PendingIntent.FLAG_UPDATE_CURRENT
        )

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
        handlePayloadImage(notification, messageData)
        handlePayloadSound(notification, messageData)
        handlePayloadButtons(notification, messageData)
        handlePayloadActions(notification, messageData)
        handlePayloadAttributes(notification, messageData)
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
            channel.setShowBadge(true)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            manager.createNotificationChannel(channel)
        }
    }

    private fun handlePayloadImage(notification: NotificationCompat.Builder, messageData: HashMap<String, String>) {
        if (messageData["image"] != null) {
            val bigImageBitmap = getBitmapFromUrl(messageData["image"]!!)
            if (bigImageBitmap != null) {
                notification.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bigImageBitmap))

            }
        }
    }

    private fun handlePayloadSound(notification: NotificationCompat.Builder, messageData: HashMap<String, String>) {
        if (messageData["sound"] != null) {
            val soundUri = Uri.parse("android.resource://" + context.packageName + "/" + messageData["sound"])
            notification.setSound(soundUri)
        }
    }

    private fun handlePayloadButtons(notification: NotificationCompat.Builder, messageData: HashMap<String, String>) {
        if (messageData["buttons"] != null) {
            val array = JSONArray(messageData["buttons"])
            for (i in 0 until array.length()) {
                val item: Map<String, String> = Gson().fromJson(array[i].toString())
                val actionEnum = ACTIONS.find(item["action"])
                val pi = generateActionPendingIntent(actionEnum, item["url"])
                notification.addAction(smallIconRes, item["title"], pi)
            }
        }
    }

    private fun handlePayloadActions(notification: NotificationCompat.Builder, messageData: HashMap<String, String>) {
        if (messageData["action"] != null) {
            val action = messageData["action"]
            val actionEnum = ACTIONS.find(action)
            val url = messageData["url"]

            if (url != null && actionEnum != null) {
                val pi = generateActionPendingIntent(actionEnum, url)
                notification.setContentIntent(pi)
            }
        }
    }

    private fun handlePayloadAttributes(notification: NotificationCompat.Builder, messageData: HashMap<String, String>) {
        if (messageData["attributes"] != null) {
            val item: Map<String, String> = Gson().fromJson(messageData["attributes"]!!)
            Logger.w(this, item.toString())
            Exponea.notificationDataCallback?.invoke(item)
        }
    }

    private fun generateActionPendingIntent(action: ACTIONS?, url: String? = null): PendingIntent? {
        return when (action) {
            ACTIONS.APP -> pendingIntent
            ACTIONS.BROWSER, ACTIONS.DEEPLINK -> {
                val actionIntent = Intent(Intent.ACTION_VIEW)
                actionIntent.data = Uri.parse(url)
                PendingIntent.getActivity(context, 0, actionIntent, 0)
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

    private enum class ACTIONS(val value: String) {
        APP("app"),
        BROWSER("browser"),
        DEEPLINK("deeplink");

        companion object {
            fun find(value: String?) = ACTIONS.values().find { it.value == value }
        }
    }
}