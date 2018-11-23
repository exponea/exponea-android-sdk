package com.exponea.sdk.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger


class ExponeaPushReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CLICKED = "com.exponea.sdk.action.PUSH_CLICKED"
        const val ACTION_DEEPLINK_CLICKED = "com.exponea.sdk.action.PUSH_DEEPLINK_CLICKED"
        const val ACTION_URL_CLICKED = "com.exponea.sdk.action.PUSH_URL_CLICKED"
        const val EXTRA_NOTIFICATION_ID = "NotificationId"
        const val EXTRA_DATA = "NotificationData"
        const val EXTRA_CUSTOM_DATA = "NotificationCustomData"
        const val EXTRA_URL = "NotificationUrl"

        fun getClickIntent(
                context: Context,
                id: Int,
                data: NotificationData?,
                messageData: HashMap<String, String>): Intent {
            return Intent(ACTION_CLICKED).apply {
                putExtra(EXTRA_NOTIFICATION_ID, id)
                putExtra(EXTRA_DATA, data)
                putExtra(EXTRA_CUSTOM_DATA, messageData)
                `package` = context.packageName
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        Logger.i(this, "Push notification clicked")

        val url = intent.getStringExtra(EXTRA_URL)
        val buttonClickedIntent = Intent(Intent.ACTION_VIEW)
        buttonClickedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (url != null && url.isNotEmpty())
            buttonClickedIntent.data = Uri.parse(url)

        when (intent.action) {
            ACTION_CLICKED -> Unit
            ACTION_DEEPLINK_CLICKED -> PendingIntent.getActivity(context, 0, buttonClickedIntent, PendingIntent.FLAG_UPDATE_CURRENT).send()
            ACTION_URL_CLICKED -> context.startActivity(buttonClickedIntent)
        }

        val data = intent.getParcelableExtra(EXTRA_DATA) as NotificationData?
        if (!Exponea.isInitialized) {
            val config = ExponeaConfigRepository.get(context)
            config?.let {
                Logger.d(this, "Newly initiated")
                Exponea.init(context.applicationContext, config)
            }
        }

        Exponea.component.pushManager.trackClickedPush(
                data = data
        )

        // After clicking the notification button (action), dismiss it
        dimissNotification(context, intent)

        // And close the notification tray
        closeNotificationTray(context)
    }

    private fun dimissNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
    }

    private fun closeNotificationTray(context: Context) {
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(it)
    }
}
