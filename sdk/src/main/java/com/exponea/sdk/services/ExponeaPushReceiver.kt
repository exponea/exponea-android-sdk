package com.exponea.sdk.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.logOnException
import kotlin.random.Random

class ExponeaPushReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CLICKED = "com.exponea.sdk.action.PUSH_CLICKED"
        const val ACTION_DEEPLINK_CLICKED = "com.exponea.sdk.action.PUSH_DEEPLINK_CLICKED"
        const val ACTION_URL_CLICKED = "com.exponea.sdk.action.PUSH_URL_CLICKED"
        const val EXTRA_NOTIFICATION_ID = "NotificationId"
        const val EXTRA_DATA = "NotificationData"
        const val EXTRA_CUSTOM_DATA = "NotificationCustomData"
        const val EXTRA_ACTION_INFO = "notification_action"
        const val EXTRA_DELIVERED_TIMESTAMP = "NotificationDeliveredTimestamp"

        fun getClickIntent(
            context: Context,
            id: Int,
            data: NotificationData?,
            messageData: HashMap<String, String>,
            deliveredTimestamp: Double?
        ): Intent {
            return Intent(ACTION_CLICKED).apply {
                putExtra(EXTRA_NOTIFICATION_ID, id)
                putExtra(EXTRA_DATA, data)
                putExtra(EXTRA_CUSTOM_DATA, messageData)
                putExtra(EXTRA_DELIVERED_TIMESTAMP, deliveredTimestamp)
                `package` = context.packageName
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            onReceiveUnsafe(context, intent)
        }.logOnException()
    }

    fun onReceiveUnsafe(
        context: Context,
        intent: Intent,
        timestamp: Double = currentTimeSeconds()
    ) {
        Logger.i(this, "Push notification clicked")

        val action = intent.getSerializableExtra(EXTRA_ACTION_INFO) as? NotificationAction?
        val buttonClickedIntent = Intent(Intent.ACTION_VIEW)
        buttonClickedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val url = action?.url
        Logger.d(this, "Interaction: $intent")

        if (url != null && url.isNotEmpty())
            buttonClickedIntent.data = Uri.parse(url)

        when (intent.action) {
            ACTION_CLICKED -> Unit
            ACTION_DEEPLINK_CLICKED -> {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    Random.nextInt(),
                    buttonClickedIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                try {
                    pendingIntent.send()
                } catch (e: PendingIntent.CanceledException) {
                    Logger.e(
                        this,
                        "Unable to handle deep-link. Make sure the deeplink schema you specified on Exponea web app" +
                            " is registered in AndroidManifest."
                    )
                }
            }
            ACTION_URL_CLICKED -> context.startActivity(buttonClickedIntent)
        }

        val data = intent.getParcelableExtra(EXTRA_DATA) as NotificationData?
        val deliveredTimestamp = intent.getDoubleExtra(EXTRA_DELIVERED_TIMESTAMP, 0.0)
        val clickedTimestamp: Double

        clickedTimestamp = if (timestamp <= deliveredTimestamp) {
            deliveredTimestamp + 1
        } else {
            timestamp
        }
        Exponea.autoInitialize(context) {
            Exponea.trackClickedPush(
                data = data,
                actionData = action,
                timestamp = clickedTimestamp
            )
            // After clicking the notification button (action), dismiss it
            dismissNotification(context, intent)

            // And close the notification tray
            closeNotificationTray(context)
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
