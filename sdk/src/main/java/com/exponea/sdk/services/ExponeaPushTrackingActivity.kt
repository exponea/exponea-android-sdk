package com.exponea.sdk.services

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_ACTION_INFO
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_CUSTOM_DATA
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_DATA
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_DELIVERED_TIMESTAMP
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_NOTIFICATION_ID
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.logOnException

internal open class ExponeaPushTrackingActivity : Activity() {

    companion object {
        fun getClickIntent(
            context: Context,
            id: Int,
            data: NotificationData?,
            messageData: HashMap<String, String>,
            deliveredTimestamp: Double?,
            olderApi: Boolean
        ): Intent {
            val klass = if (olderApi) {
                ExponeaPushTrackingActivityOlderApi::class.java
            } else {
                ExponeaPushTrackingActivity::class.java
            }
            return Intent(context, klass).apply {
                putExtra(EXTRA_NOTIFICATION_ID, id)
                putExtra(EXTRA_DATA, data)
                putExtra(EXTRA_CUSTOM_DATA, messageData)
                putExtra(EXTRA_DELIVERED_TIMESTAMP, deliveredTimestamp)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                `package` = context.packageName
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            processPushClick(this, intent)
        }.logOnException()
        finish()
    }

    open fun processPushClick(
        context: Context,
        intent: Intent,
        timestamp: Double = currentTimeSeconds()
    ) {
        Logger.i(this, "Push notification clicked")

        val action = intent.getSerializableExtra(EXTRA_ACTION_INFO) as? NotificationAction?
        Logger.d(this, "Interaction: $action")

        val data = intent.getParcelableExtra(EXTRA_DATA) as NotificationData?
        val deliveredTimestamp = intent.getDoubleExtra(EXTRA_DELIVERED_TIMESTAMP, 0.0)

        val clickedTimestamp: Double = if (timestamp <= deliveredTimestamp) {
            deliveredTimestamp + 1
        } else {
            timestamp
        }
        Exponea.autoInitialize(context) {
            if (data?.hasTrackingConsent == true || action?.isTrackingForced == true) {
                Exponea.trackClickedPush(
                    data = data,
                    actionData = action,
                    timestamp = clickedTimestamp
                )
            } else {
                Logger.e(this,
                    "Event for clicked notification is not tracked because consent is not given nor forced")
            }

            // After clicking the notification button (action), dismiss it
            dismissNotification(context, intent)

            // And close the notification tray
            closeNotificationTray(context)
        }

        // send also broadcast with this action, so client app can also react to push open event

        Intent().also { broadcastIntent ->
            broadcastIntent.action = intent.action
            broadcastIntent.putExtra(EXTRA_ACTION_INFO, action)
            broadcastIntent.putExtra(EXTRA_DATA, data)
            broadcastIntent.putExtra(EXTRA_CUSTOM_DATA, intent.getSerializableExtra(EXTRA_CUSTOM_DATA))
            broadcastIntent.`package` = context.packageName
            PendingIntent.getBroadcast(
                context,
                0,
                broadcastIntent,
                MessagingUtils.getPendingIntentFlags()
            ).send()
        }
    }

    private fun dismissNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
    }

    private fun closeNotificationTray(context: Context) {
        // target version is hardcoded until compileSDKversion is increased
        if (context.applicationInfo.targetSdkVersion <= 30) {
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            context.sendBroadcast(it)
        }
    }
}
