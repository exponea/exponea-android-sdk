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
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.repository.FirebaseTokenRepository
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.adjustUrl
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.io.IOException
import java.net.URL
import kotlin.concurrent.thread


internal class FcmManagerImpl(
        private val context: Context,
        private val configuration: ExponeaConfiguration,
        private val firebaseTokenRepository: FirebaseTokenRepository
) : FcmManager {

    private lateinit var pendingIntent: PendingIntent
    private lateinit var pushReceiverIntent: Intent
    private val requestCode = 111
    private var smallIconRes = -1

    override fun trackFcmToken(token: String?) {
        val lastTrackDateInMilliseconds =
            firebaseTokenRepository.getLastTrackDateInMilliseconds() ?: System.currentTimeMillis()
        val shouldUpdateToken = when (Exponea.tokenTrackFrequency) {
            ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE -> token != firebaseTokenRepository.get()
            ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH -> true
            ExponeaConfiguration.TokenFrequency.DAILY -> !DateUtils.isToday(lastTrackDateInMilliseconds)
        }

        if (token != null && shouldUpdateToken) {
            firebaseTokenRepository.set(token, System.currentTimeMillis())
            Exponea.trackPushToken(token)
            return
        }

        Logger.d(this, "Token not update: shouldUpdateToken $shouldUpdateToken - token $token")

    }

    override fun handleRemoteMessage(message: RemoteMessage?, manager: NotificationManager, showNotification: Boolean) {

        Logger.d(this, "handleRemoteMessage")

        if (message == null) return

        val title = message.data?.get("title") ?: ""
        val body = message.data?.get("message") ?: ""

        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        val dataString = message.data?.get("data") ?: message.data?.get("attributes")
        val data = gson.fromJson(dataString, NotificationData::class.java)
        val notificationId = message.data?.get("notification_id")?.toInt() ?: 0

        // Configure the notification channel for push notifications on API 26+
        // This configuration runs only once.
        if (!Exponea.component.pushNotificationRepository.get()) {
            createNotificationChannel(manager)
            Exponea.component.pushNotificationRepository.set(true)
        }

        // Track the delivered push event to Exponea API.
        Exponea.trackDeliveredPush(
                data = data
        )

        if (showNotification && isValidNotification(message)) {
            //Create a map with all the data of the remote message, removing the data already processed
            val messageData = message.data?.apply {
                remove("title")
                remove("message")
                remove("data")
                remove("notification_id")
            }?.run {
                HashMap(this)
            } ?: HashMap()

            // Show push notification.
            showNotification(
                    title,
                    body,
                    data,
                    notificationId,
                    manager,
                    messageData
            )
        }

    }

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

    private fun isValidNotification(message: RemoteMessage?): Boolean {
        return (message?.data?.get("title")?.isNotBlank() == true) || (message?.data?.get("message")?.isNotBlank() == true)
    }

    private fun handlePayload(notification: NotificationCompat.Builder, messageData: HashMap<String, String>) {
        val notificationPayload = NotificationPayload(messageData)
        handlePayloadImage(notification, notificationPayload)
        handlePayloadSound(notification, notificationPayload)
        handlePayloadButtons(notification, notificationPayload)
        handlePayloadNotificationAction(notification, notificationPayload)
        handlePayloadAttributes(notificationPayload)
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
            messageData.buttons.forEachIndexed { index, it ->
                val info = NotificationAction(NotificationAction.ACTION_TYPE_BUTTON, it.title, it.url.adjustUrl())
                val pi = generateActionPendingIntent(it.action, info, index)
                notification.addAction(0, it.title, pi)
            }
        }
    }

    private fun handlePayloadNotificationAction(notification: NotificationCompat.Builder, messageData: NotificationPayload) {
        //handle the notification body click action
        messageData.notificationAction?.let {
            val info = NotificationAction(NotificationAction.ACTION_TYPE_NOTIFICATION, it.title, it.url.adjustUrl())
            val pi = generateActionPendingIntent(it.action, info, requestCode)
            notification.setContentIntent(pi)
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

    private fun generateActionPendingIntent(action: NotificationPayload.Actions?, actionInfo: NotificationAction, requestCode: Int): PendingIntent? {
        val actionIntent = pushReceiverIntent
        actionIntent.putExtra(ExponeaPushReceiver.EXTRA_ACTION_INFO, actionInfo)

        return when (action) {
            NotificationPayload.Actions.APP -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_CLICKED
                PendingIntent.getBroadcast(context, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            NotificationPayload.Actions.BROWSER -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_URL_CLICKED
                PendingIntent.getBroadcast(context, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            NotificationPayload.Actions.DEEPLINK -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_DEEPLINK_CLICKED
                PendingIntent.getBroadcast(context, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            else -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_CLICKED
                PendingIntent.getBroadcast(context, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
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

