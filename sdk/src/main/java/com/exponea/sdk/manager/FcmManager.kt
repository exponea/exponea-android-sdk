package com.exponea.sdk.manager

import android.app.NotificationManager
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationChannelImportance
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.util.TokenType
import com.exponea.sdk.util.currentTimeSeconds

internal interface FcmManager {
    fun trackToken(
        token: String? = null,
        tokenTrackFrequency: ExponeaConfiguration.TokenFrequency?,
        tokenType: TokenType?
    )
    fun handleRemoteMessage(
        messageData: Map<String, String>?,
        manager: NotificationManager,
        showNotification: Boolean = true,
        timestamp: Double = currentTimeSeconds()
    )
    fun showNotification(manager: NotificationManager, payload: NotificationPayload)
    fun findNotificationChannelImportance(): NotificationChannelImportance
}
