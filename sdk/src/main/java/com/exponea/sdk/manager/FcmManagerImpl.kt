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
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaExtras
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaNotificationActionType
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationChannelImportance
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.receiver.NotificationsPermissionReceiver
import com.exponea.sdk.repository.PushTokenRepository
import com.exponea.sdk.services.ExponeaPushTrackingActivity
import com.exponea.sdk.services.MessagingUtils
import com.exponea.sdk.telemetry.model.TelemetryEvent
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.TokenType
import com.exponea.sdk.util.adjustUrl
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.Random
import kotlin.concurrent.thread

/**
 * Handles data in scope of Push notifications integration (currently FCM and HMS services).
 *
 * !!! Keep behavior in sync with TimeLimitedFcmManagerImpl
 */
internal open class FcmManagerImpl(
    context: Context,
    private val configuration: ExponeaConfiguration,
    private val eventManager: EventManager,
    private val pushTokenRepository: PushTokenRepository,
    internal val trackingConsentManager: TrackingConsentManager
) : FcmManager {
    private val application = context.applicationContext
    private val requestCodeGenerator: Random = Random()
    private var lastPushNotificationId: Int? = null

    override fun trackToken(
        token: String?,
        tokenTrackFrequency: ExponeaConfiguration.TokenFrequency?,
        tokenType: TokenType?,
        isTokenCanceled: Boolean
    ) {
        if (Exponea.isStopped) {
            Logger.e(this, "Push token track failed, SDK is stopping")
            return
        }
        val permissionGranted = NotificationsPermissionReceiver.isPermissionGranted(application)
        val permissionRequired = configuration.requirePushAuthorization
        val permissionMismatched = permissionRequired && !permissionGranted
        val shouldUpdateToken = run {
            val lastTrackDateInMilliseconds = pushTokenRepository.getLastTrackDateInMilliseconds()

            if (isTokenCanceled) {
                true
            } else if (lastTrackDateInMilliseconds == null) {
                // if the token wasn't ever tracked, track it
                true
            } else if (permissionMismatched) {
                // if permission expectation mismatched, track (empty token)
                true
            } else {
                // decide by the frequency
                when (tokenTrackFrequency ?: configuration.tokenTrackFrequency) {
                    ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE -> {
                        token != pushTokenRepository.get() ||
                                permissionGranted != pushTokenRepository.getLastPermissionFlag()
                    }
                    ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH -> true
                    ExponeaConfiguration.TokenFrequency.DAILY -> !DateUtils.isToday(lastTrackDateInMilliseconds)
                }
            }
        }
        if (token != null && tokenType != null && shouldUpdateToken) {
            pushTokenRepository.setTrackedToken(
                token,
                System.currentTimeMillis(),
                tokenType,
                permissionGranted
            )

            val validityResult = when {
                isTokenCanceled -> false
                else -> !permissionMismatched
            }
            val validityMessage = when {
                isTokenCanceled -> Constants.PushPermissionStatus.INVALIDATED_TOKEN
                permissionMismatched -> Constants.PushPermissionStatus.PERMISSION_DENIED
                else -> Constants.PushPermissionStatus.PERMISSION_GRANTED
            }
            val properties = PropertiesList(hashMapOf(
                "push_notification_token" to token,
                "platform" to tokenType.selfCheckProperty,
                "valid" to validityResult,
                "description" to validityMessage
            ))

            eventManager.track(
                eventType = Constants.EventTypes.pushTokenTrack,
                properties = properties.properties,
                type = EventType.PUSH_TOKEN
            )
            return
        }

        Logger.d(this, "Token was not updated: shouldUpdateToken $shouldUpdateToken - token $token")
    }

    override fun handleRemoteMessage(
        messageData: Map<String, String>?,
        manager: NotificationManager,
        showNotification: Boolean,
        timestamp: Double
    ) {
        Logger.d(this, "handleRemoteMessage")
        if (!configuration.automaticPushNotification) {
            Logger.w(this, "Notification delivery not handled," +
                " initialized SDK configuration has 'automaticPushNotification' == false")
            return
        }
        ensureNotificationChannelExistence(application, manager)
        if (messageData == null) {
            Logger.w(this, "Push notification not handled because of no data")
            return
        }
        val payload = parseNotificationPayload(messageData, timestamp)
        if (payload.deliveredTimestamp == null) {
            // this really should not happen, parsing must find a proper delivery time
            Logger.e(this, "Push notification needs info about time delivery")
            payload.deliveredTimestamp = timestamp
        }
        if (payload.notificationAction.action == ExponeaNotificationActionType.SELFCHECK) {
            Logger.d(this, "Self-check notification received")
            onSelfCheckReceived()
            return
        }
        val notificationChannelImportance = findNotificationChannelImportance(manager)
        if (MessagingUtils.areNotificationsBlockedForTheApp(application, manager, configuration.pushChannelId)) {
            Logger.w(
                this,
                "Notification delivery not handled, notifications for the app are turned off in the settings"
            )
            trackDeliveredPush(
                payload,
                payload.deliveredTimestamp!!,
                Constants.PushNotifShownStatus.NOT_SHOWN,
                notificationChannelImportance
            )
            return
        }
        if (payload.notificationId == lastPushNotificationId) {
            Logger.i(this, "Ignoring push notification with id ${payload.notificationId} that was already received.")
            return
        }
        lastPushNotificationId = payload.notificationId
        if (showNotification && !payload.silent && (payload.title.isNotBlank() || payload.message.isNotBlank())) {
            trackDeliveredPush(
                payload,
                payload.deliveredTimestamp!!,
                Constants.PushNotifShownStatus.SHOWN,
                notificationChannelImportance
            )
            showNotification(manager, payload)
            Exponea.telemetry?.reportEvent(TelemetryEvent.PUSH_NOTIFICATION_SHOWN, hashMapOf(
                "notificationId" to payload.notificationId.toString(),
                "actionId" to (payload.notificationData.getTrackingData()["action_id"]?.toString() ?: ""),
                "campaignId" to (payload.notificationData.getTrackingData()["campaign_id"]?.toString() ?: "")
            ))
        } else {
            trackDeliveredPush(
                payload,
                payload.deliveredTimestamp!!,
                Constants.PushNotifShownStatus.NOT_SHOWN,
                notificationChannelImportance
            )
        }
    }

    override fun findNotificationChannelImportance(): NotificationChannelImportance {
        return findNotificationChannelImportance(
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        )
    }

    private fun findNotificationChannelImportance(manager: NotificationManager): NotificationChannelImportance {
        val notificationChannelImportance = MessagingUtils.getNotificationChannelImportance(
            application,
            manager,
            configuration.pushChannelId
        )
        return notificationChannelImportance
    }

    private fun parseNotificationPayload(
        source: Map<String, String>,
        deviceReceivedTimestamp: Double
    ): NotificationPayload {
        val payload = NotificationPayload(HashMap(source))
        val sentTimestamp = payload.notificationData.sentTimestamp
        val deliveredTimestamp = if (sentTimestamp != null && deviceReceivedTimestamp <= sentTimestamp) {
            sentTimestamp + 1
        } else {
            deviceReceivedTimestamp
        }
        payload.deliveredTimestamp = deliveredTimestamp
        return payload
    }

    protected open fun onSelfCheckReceived() {
        Exponea.selfCheckPushReceived()
    }

    protected open fun trackDeliveredPush(
        payload: NotificationPayload,
        deliveredTimestamp: Double,
        shownStatus: Constants.PushNotifShownStatus,
        notificationChannelImportance: NotificationChannelImportance
    ) {
        if (payload.notificationData.hasTrackingConsent) {
            trackingConsentManager.trackDeliveredPush(
                data = payload.notificationData,
                timestamp = deliveredTimestamp,
                mode = TrackingConsentManager.MODE.CONSIDER_CONSENT,
                shownStatus = shownStatus,
                notificationChannelImportance = notificationChannelImportance
            )
        } else {
            Logger.i(this, "Event for delivered notification is not tracked because consent is not given")
        }
        Exponea.notifyCallbacksForNotificationDelivery(payload)
        Exponea.telemetry?.reportEvent(TelemetryEvent.PUSH_NOTIFICATION_DELIVERED, hashMapOf(
            "notificationId" to payload.notificationId.toString(),
            "actionId" to (payload.notificationData.getTrackingData()["action_id"]?.toString() ?: ""),
            "campaignId" to (payload.notificationData.getTrackingData()["campaign_id"]?.toString() ?: "")
        ))
    }

    override fun showNotification(manager: NotificationManager, payload: NotificationPayload) {
        Logger.d(this, "showNotification")
        ensureNotificationChannelExistence(application, manager)
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

    internal fun ensureNotificationChannelExistence(context: Context, manager: NotificationManager) {
        // Configure the notification channel for push notifications on API 26+
        // This configuration runs only once.
        if (!MessagingUtils.doesChannelExists(context, manager, configuration.pushChannelId)) {
            createNotificationChannel(manager)
        }
    }

    private fun createNotificationChannel(manager: NotificationManager) {
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
        if (messageData.sound != null) {
            val soundFileName: String = if (File(messageData.sound).extension.isEmpty()) {
                messageData.sound
            } else {
                File(messageData.sound).nameWithoutExtension
            }
            // if the raw file exists, use it as custom sound
            if (application.resources.getIdentifier(soundFileName, "raw", application.packageName) != 0) {
                soundUri = Uri.parse(
                    """android.resource://${application.packageName}/raw/$soundFileName"""
                )
            }
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
            val channel = MessagingUtils.getNotificationChannel(application, manager, configuration.pushChannelId)
            if (channel == null) {
                Logger.d(this, "Won't play notification sound, channel not found.")
                shouldPlaySound = false
            } else if (channel.importance <= NotificationManager.IMPORTANCE_LOW) {
                Logger.d(this, "Won't play notification sound, channel has low importance.")
                shouldPlaySound = false
            }

            if (shouldPlaySound) {
                try {
                    playNotificationSound(soundUri)
                } catch (e: Throwable) {
                    Logger.e(this, "Failed to play notification sound", e)
                    // playing fallback - default one
                    playNotificationSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                }
            }
        }
    }

    private fun playNotificationSound(soundUri: Uri?) {
        RingtoneManager.getRingtone(application, soundUri)?.play()
    }

    private fun handlePayloadButtons(
        notification: NotificationCompat.Builder,
        payload: NotificationPayload
    ) {
        if (payload.buttons != null) {
            // if we have a button payload, verify each button action
            for (button in payload.buttons) {
                val info = NotificationAction(
                    NotificationAction.ACTION_TYPE_BUTTON,
                    button.title,
                    button.url.adjustUrl()
                )
                val pi = generateActionPendingIntent(payload, button.action, info, requestCodeGenerator.nextInt())
                notification.addAction(0, button.title, pi)
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

    private fun getPushReceiverIntent(payload: NotificationPayload): Intent {
        return ExponeaPushTrackingActivity.getClickIntent(
            application,
            payload.notificationId,
            payload.notificationData,
            payload.rawData,
            payload.deliveredTimestamp
        )
    }

    private fun generateActionPendingIntent(
        payload: NotificationPayload,
        action: ExponeaNotificationActionType?,
        actionInfo: NotificationAction,
        requestCode: Int
    ): PendingIntent? {
        val trackingIntent = getPushReceiverIntent(payload)
        trackingIntent.putExtra(ExponeaExtras.EXTRA_ACTION_INFO, actionInfo)

        return when (action) {
            ExponeaNotificationActionType.BROWSER -> {
                trackingIntent.action = ExponeaExtras.ACTION_URL_CLICKED
                getUrlIntent(requestCode, trackingIntent, actionInfo.url)
            }
            ExponeaNotificationActionType.DEEPLINK -> {
                trackingIntent.action = ExponeaExtras.ACTION_DEEPLINK_CLICKED
                getUrlIntent(requestCode, trackingIntent, actionInfo.url)
            }
            // NotificationPayload.Actions.APP
            else -> {
                trackingIntent.action = ExponeaExtras.ACTION_CLICKED
                getAppIntent(requestCode, trackingIntent)
            }
        }
    }

    private fun getAppIntent(
        requestCode: Int,
        trackingIntent: Intent
    ): PendingIntent? {
        if (MessagingUtils.multiPendingIntentsAllowed()) {
            val launchIntent = MessagingUtils.getIntentAppOpen(application)
            return PendingIntent.getActivities(
                application,
                requestCode,
                arrayOf(launchIntent, trackingIntent),
                MessagingUtils.getPendingIntentFlags()
            )
        }
        // for older SDKs, launch intent will be started directly from the tracking activity
        return PendingIntent.getActivity(
            application,
            requestCode,
            trackingIntent,
            MessagingUtils.getPendingIntentFlags()
        )
    }

    private fun getUrlIntent(
        requestCode: Int,
        trackingIntent: Intent,
        url: String?
    ): PendingIntent {
        if (MessagingUtils.multiPendingIntentsAllowed()) {
            val urlIntent = Intent(Intent.ACTION_VIEW)
            urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            if (!url.isNullOrEmpty()) urlIntent.data = Uri.parse(url)
            return PendingIntent.getActivities(
                application,
                requestCode,
                arrayOf(urlIntent, trackingIntent),
                MessagingUtils.getPendingIntentFlags()
            )
        }
        // for older SDKs, web and deeplink intent will be started directly from the tracking activity
        return PendingIntent.getActivity(
            application,
            requestCode,
            trackingIntent,
            MessagingUtils.getPendingIntentFlags()
        )
    }

    protected open fun getBitmapFromUrl(url: String): Bitmap? {
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
