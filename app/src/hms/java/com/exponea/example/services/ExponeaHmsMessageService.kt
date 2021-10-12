package com.exponea.example.services

import android.app.NotificationManager
import android.content.Context
import com.exponea.sdk.Exponea
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

class ExponeaHmsMessageService : HmsMessageService() {

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Exponea.handleRemoteMessage(applicationContext, message.dataOfMap, notificationManager)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // replace with handleNewHmsToken once backend is ready
        Exponea.handleNewHmsToken(applicationContext, token)
    }
}
