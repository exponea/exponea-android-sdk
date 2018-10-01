package com.exponea.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import com.exponea.example.view.MainActivity
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.services.ExponeaPushReceiver

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            ExponeaPushReceiver.ACTION_CLICKED -> {

                // Extract payload data
                val data = intent.getParcelableExtra<NotificationData>(ExponeaPushReceiver.EXTRA_DATA)
                Log.i("Receiver", "Payload: $data")

                // Act upon push receiving
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(context, launchIntent, null)
            }
        }
    }
}
