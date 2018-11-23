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

        when (intent.action) {
            ACTION_CLICKED -> {
            }
            ACTION_DEEPLINK_CLICKED -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (url.isNotEmpty()) {
                    val i = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    PendingIntent.getActivity(context, 0, i, 0).send()
                }
            }
            ACTION_URL_CLICKED -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (url.isNotEmpty()) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(browserIntent)
                }
            }
        }

        val data = intent.getParcelableExtra(EXTRA_DATA) as NotificationData?
        if (!Exponea.isInitialized) {
            val config = ExponeaConfigRepository.get(context)
            if (config != null) {
                Logger.d(this, "Newly initiated")
                Exponea.init(context.applicationContext, config)
            }
        }

        Exponea.component.pushManager.trackClickedPush(
                data = data
        )

        // After clicking the notification button (action), dismiss it
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
        // And close the notification tray
        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(it)
    }
}
