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
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.FirebaseTokenRepository
import com.exponea.sdk.repository.PushNotificationRepository
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.adjustUrl
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException
import java.net.URL
import java.util.Random
import kotlin.concurrent.thread

internal class FcmManagerImpl(
    context: Context,
    private val configuration: ExponeaConfiguration,
    private val eventManager: EventManager,
    private val firebaseTokenRepository: FirebaseTokenRepository,
    private val pushNotificationRepository: PushNotificationRepository
) : FcmManager {
    private val application = context.applicationContext
    private val requestCodeGenerator: Random = Random()
    private var lastPushNotificationId: Int? = null

    override fun trackFcmToken(token: String?, tokenTrackFrequency: ExponeaConfiguration.TokenFrequency) {
        val shouldUpdateToken = run {
            val lastTrackDateInMilliseconds = firebaseTokenRepository.getLastTrackDateInMilliseconds()
            if (lastTrackDateInMilliseconds == null) { // if the token wasn't ever tracked, track it
                true
            } else {
                when (tokenTrackFrequency) {
                    ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE -> token != firebaseTokenRepository.get()
                    ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH -> true
                    ExponeaConfiguration.TokenFrequency.DAILY -> !DateUtils.isToday(lastTrackDateInMilliseconds)
                }
            }
        }

        if (token != null && shouldUpdateToken) {
            firebaseTokenRepository.set(token, System.currentTimeMillis())
            val properties = PropertiesList(hashMapOf("google_push_notification_id" to token))
            eventManager.track(
                eventType = Constants.EventTypes.push,
                properties = properties.properties,
                type = EventType.PUSH_TOKEN
            )
            return
        }

        Logger.d(this, "Token was not updated: shouldUpdateToken $shouldUpdateToken - token $token")
    }

    override fun handleRemoteMessage(
        message: RemoteMessage?,
        manager: NotificationManager,
        showNotification: Boolean,
        timestamp: Double
    ) {

        Logger.d(this, "handleRemoteMessage")

        if (message == null) return

        // Configure the notification channel for push notifications on API 26+
        // This configuration runs only once.
        if (!pushNotificationRepository.get()) {
            createNotificationChannel(manager)
            pushNotificationRepository.set(true)
        }

        var data = HashMap(message.data)
        // data["attributes"] = "{\"new_key\": 3534646246.3461352351235134742574634 ,\"another\": {\"nested_one\": 3534646246.3461352351235134742574634,\"nested_two\":\"hi this is string value\"}}"

        val payload = NotificationPayload(data)
        val sentTimestamp = payload.notificationData.sentTimestamp

        val deliveredTimestamp = if (sentTimestamp != null && timestamp <= sentTimestamp) {
            sentTimestamp + 1
        } else {
            timestamp
        }
        payload.deliveredTimestamp = deliveredTimestamp

        if (payload.notificationAction.action == NotificationPayload.Actions.SELFCHECK) {
            Exponea.selfCheckPushReceived()
            return
        }

        Exponea.trackDeliveredPush(data = payload.notificationData, timestamp = deliveredTimestamp)

        if (payload.notificationId == lastPushNotificationId) {
            Logger.i(this, "Ignoring push notification with id ${payload.notificationId} that was already received.")
        } else {
            lastPushNotificationId = payload.notificationId
            callNotificationDataCallback(payload)
            if (showNotification && !payload.silent && (payload.title.isNotBlank() || payload.message.isNotBlank())) {
                showNotification(manager, payload)
            }
        }
    }

    override fun showNotification(manager: NotificationManager, payload: NotificationPayload) {
        Logger.d(this, "showNotification")

        val notification = NotificationCompat.Builder(application, configuration.pushChannelId)
            .setContentText(payload.message)
            .setContentTitle(payload.title)
            .setChannelId(configuration.pushChannelId)
            .setSmallIcon(getPushIconRes())
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.message))
        configuration.pushAccentColor?.let { notification.color = it }

        handlePayloadImage(notification, payload)
        handlePayloadSound(manager, notification, payload)
        handlePayloadButtons(notification, payload)
        handlePayloadNotificationAction(notification, payload)

        manager.notify(payload.notificationId, notification.build())
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

    private fun handlePayloadImage(
        notification: NotificationCompat.Builder,
        messageData: NotificationPayload
    ) {
        // Load the image in the payload and add as a big picture in the notification
        if (messageData.image != null) {
            val bigImageBitmap = getBitmapFromUrl(messageData.image)
            // verify if the image was successfully loaded
            if (bigImageBitmap != null) {
                notification.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bigImageBitmap))
            }
        }
    }

    private fun getPushIconRes(): Int {
        // If push icon was not provided in the configuration, default one will be used
        var smallIconRes = configuration.pushIcon ?: android.R.drawable.ic_dialog_info

        try {
            application.resources.getResourceName(smallIconRes)
        } catch (exception: Resources.NotFoundException) {
            Logger.e(this, "Invalid icon resource: $smallIconRes")
            smallIconRes = android.R.drawable.ic_dialog_info
        }
        return smallIconRes
    }

    private fun handlePayloadSound(
        manager: NotificationManager,
        notification: NotificationCompat.Builder,
        messageData: NotificationPayload
    ) {
        // set the uri for the default sound
        var soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // if the raw file exists, use it as custom sound
        if (messageData.sound != null &&
            application.resources.getIdentifier(messageData.sound, "raw", application.packageName) != 0
        ) {
            soundUri = Uri.parse("""android.resource://${application.packageName}/raw/${messageData.sound}""")
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // on older versions of Android, we can set sound directly on the notification
            Logger.d(this, "Setting notification sound directly on notification since device is pre-Oreo")
            notification.setSound(soundUri)
        } else {
            // Since sounds should be set on a channel on newer OS and we want to
            // change them per notification, we have to play sound manually
            // We only play sound if the notification should be displayed
            var shouldPlaySound = true

            // if DnD is on
            if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                Logger.d(this, "Won't play notification sound, DnD mode is on")
                shouldPlaySound = false
            }

            // if application is not allowed to show notifications
            if (!manager.areNotificationsEnabled()) {
                Logger.d(this, "Won't play notification sound, notifications are not allowed")
                shouldPlaySound = false
            }

            // if the notification channel is blocked/deleted
            val channel = manager.getNotificationChannel(configuration.pushChannelId)
            if (channel == null) {
                Logger.d(this, "Won't play notification sound, channel not found.")
                shouldPlaySound = false
            } else if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                Logger.d(this, "Won't play notification sound, channel is blocked.")
                shouldPlaySound = false
            }

            if (shouldPlaySound) {
                RingtoneManager.getRingtone(application, soundUri)?.play()
            }
        }
    }

    private fun handlePayloadButtons(
        notification: NotificationCompat.Builder,
        payload: NotificationPayload
    ) {
        if (payload.buttons != null) {
            // if we have a button payload, verify each button action
            payload.buttons.forEach {
                val info = NotificationAction(NotificationAction.ACTION_TYPE_BUTTON, it.title, it.url.adjustUrl())
                val pi = generateActionPendingIntent(payload, it.action, info, requestCodeGenerator.nextInt())
                notification.addAction(0, it.title, pi)
            }
        }
    }

    private fun handlePayloadNotificationAction(
        notification: NotificationCompat.Builder,
        payload: NotificationPayload
    ) {
        // handle the notification body click action
        payload.notificationAction.let {
            val info = NotificationAction(NotificationAction.ACTION_TYPE_NOTIFICATION, it.title, it.url.adjustUrl())
            val pi = generateActionPendingIntent(payload, it.action, info, requestCodeGenerator.nextInt())
            notification.setContentIntent(pi)
        }
    }

    private fun callNotificationDataCallback(messageData: NotificationPayload) {
        if (messageData.attributes != null) {
            if (Exponea.notificationDataCallback == null) {
                pushNotificationRepository.setExtraData(messageData.attributes)
                return
            }
            Handler(Looper.getMainLooper()).post {
                Exponea.notificationDataCallback?.invoke(messageData.attributes)
            }
        }
    }

    private fun getPushReceiverIntent(payload: NotificationPayload): Intent {
        return ExponeaPushReceiver.getClickIntent(
            application,
            payload.notificationId,
            payload.notificationData,
            payload.rawData,
            payload.deliveredTimestamp
        )
    }

    private fun generateActionPendingIntent(
        payload: NotificationPayload,
        action: NotificationPayload.Actions?,
        actionInfo: NotificationAction,
        requestCode: Int
    ): PendingIntent? {
        val actionIntent = getPushReceiverIntent(payload)
        actionIntent.putExtra(ExponeaPushReceiver.EXTRA_ACTION_INFO, actionInfo)

        return when (action) {
            NotificationPayload.Actions.APP -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_CLICKED
                PendingIntent.getBroadcast(application, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            NotificationPayload.Actions.BROWSER -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_URL_CLICKED
                PendingIntent.getBroadcast(application, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            NotificationPayload.Actions.DEEPLINK -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_DEEPLINK_CLICKED
                PendingIntent.getBroadcast(application, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            else -> {
                actionIntent.action = ExponeaPushReceiver.ACTION_CLICKED
                PendingIntent.getBroadcast(application, requestCode, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
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
