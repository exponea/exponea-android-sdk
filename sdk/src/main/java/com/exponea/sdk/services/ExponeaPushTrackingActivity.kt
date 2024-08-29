package com.exponea.sdk.services

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_CUSTOM_DATA
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_DATA
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_DELIVERED_TIMESTAMP
import com.exponea.sdk.ExponeaExtras.Companion.EXTRA_NOTIFICATION_ID
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.logOnException

internal open class ExponeaPushTrackingActivity : Activity() {

    companion object {
        fun getClickIntent(
            context: Context,
            id: Int,
            data: NotificationData?,
            messageData: HashMap<String, String>,
            deliveredTimestamp: Double?
        ): Intent {
            return Intent(context, MessagingUtils.determinePushTrackingActivityClass()).apply {
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

    open fun processPushClick(context: Context, intent: Intent) {
        Logger.i(this, "Push notification clicked")
        Exponea.processPushNotificationClickInternally(intent)
        dismissNotification(context, intent)
        closeNotificationTray(context)
    }

    private fun dismissNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
    }

    private fun closeNotificationTray(context: Context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            context.sendBroadcast(it)
        }
    }
}
