package com.exponea.example.services

import android.app.NotificationManager
import android.content.Context
import com.exponea.sdk.Exponea
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ExponeaFirebaseMessageService : FirebaseMessagingService() {

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Exponea.handleRemoteMessage(applicationContext, message.data, notificationManager)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Exponea.handleNewToken(applicationContext, token)
    }
}
