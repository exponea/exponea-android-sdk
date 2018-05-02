package com.exponea.sdk.manager

import android.app.NotificationManager

interface FcmManager {
    val fcmToken: String

    fun trackFcmToken()
    fun showNotification(title: String, message: String, id: Int, manager: NotificationManager)
    fun createNotificationChannel(manager: NotificationManager)
}