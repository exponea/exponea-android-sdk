package com.exponea.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.exponea.example.logging.LogEntry
import com.exponea.example.logging.MemoryLogger
import com.exponea.sdk.ExponeaExtras
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData

class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<NotificationData>(ExponeaExtras.EXTRA_DATA)
        @Suppress("DEPRECATION")
        val actionInfo = intent.getSerializableExtra(ExponeaExtras.EXTRA_ACTION_INFO) as? NotificationAction
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        val customData = intent.getSerializableExtra(ExponeaExtras.EXTRA_CUSTOM_DATA) as? Map<String, String>

        MemoryLogger.INSTANCE.append(
            LogEntry.Level.INFO,
            "MyReceiver",
            "Push action received: data=$data action=$actionInfo custom=$customData"
        )
    }
}
