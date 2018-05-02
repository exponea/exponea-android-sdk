package com.exponea.sdk.manager

import android.app.NotificationManager

interface FcmManager {
    fun getFcmToken(): String
    fun trackFcmToken()
    fun showNotification(title: String, message: String, id: Int, manager: NotificationManager)
}