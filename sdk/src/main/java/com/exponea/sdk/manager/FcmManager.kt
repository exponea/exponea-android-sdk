package com.exponea.sdk.manager

import android.app.NotificationManager
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.util.currentTimeSeconds
import com.google.firebase.messaging.RemoteMessage

internal interface FcmManager {
    fun trackFcmToken(token: String? = null, tokenTrackFrequency: ExponeaConfiguration.TokenFrequency)
    fun handleRemoteMessage(
        message: RemoteMessage?,
        manager: NotificationManager,
        showNotification: Boolean = true,
        timestamp: Double = currentTimeSeconds()
    )
    fun showNotification(manager: NotificationManager, payload: NotificationPayload)
    fun createNotificationChannel(manager: NotificationManager)
}
