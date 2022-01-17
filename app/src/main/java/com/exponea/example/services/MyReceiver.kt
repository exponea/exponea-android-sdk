package com.exponea.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.exponea.sdk.ExponeaExtras
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData

class MyReceiver : BroadcastReceiver() {

    // React on push action
    override fun onReceive(context: Context, intent: Intent) {
        // Extract push data
        val data = intent.getParcelableExtra<NotificationData>(ExponeaExtras.EXTRA_DATA)
        val actionInfo = intent.getSerializableExtra(ExponeaExtras.EXTRA_ACTION_INFO) as? NotificationAction
        val customData = intent.getSerializableExtra(ExponeaExtras.EXTRA_CUSTOM_DATA) as Map<String, String>

        // Process push data as you need
        print(customData)
    }
}
