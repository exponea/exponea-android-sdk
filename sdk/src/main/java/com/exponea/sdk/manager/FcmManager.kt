package com.exponea.sdk.manager

import android.app.NotificationManager
import com.exponea.sdk.models.CustomerIds

interface FcmManager {
    val fcmToken: String

    fun showNotification(title: String, message: String, id: Int, manager: NotificationManager)
    fun createNotificationChannel(manager: NotificationManager)
    fun trackFcmToken()
    fun trackDeliveredPush(customerIds: CustomerIds, fcmToken: String)
    fun trackClickedPush(customerIds: CustomerIds, fcmToken: String)
}