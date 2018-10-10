package com.exponea.sdk.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger

class ExponeaPushReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CLICKED = "com.exponea.sdk.action.PUSH_CLICKED"
        const val EXTRA_NOTIFICATION_ID = "NotificationId"
        const val EXTRA_DATA = "NotificationData"
        const val EXTRA_CUSTOM_DATA = "NotificationCustomData"

        fun getClickIntent(
                context: Context,
                id: Int,
                data: NotificationData?,
                messageData : HashMap<String, String>) : Intent {
            return Intent(ACTION_CLICKED).apply {
                putExtra(EXTRA_NOTIFICATION_ID, id)
                putExtra(EXTRA_DATA, data)
                putExtra(EXTRA_CUSTOM_DATA, messageData)
                `package` = context.packageName
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CLICKED -> {
                Logger.i(this, "Push notification clicked")
                val data = intent.getParcelableExtra(EXTRA_DATA) as NotificationData
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
            }
        }
    }
}
