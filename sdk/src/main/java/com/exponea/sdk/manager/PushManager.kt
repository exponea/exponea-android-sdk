package com.exponea.sdk.manager

import com.exponea.sdk.models.NotificationData

interface PushManager {
    val fcmToken: String?

    fun trackFcmToken(token: String? = null)
    fun trackDeliveredPush(data: NotificationData? = null)
    fun trackClickedPush(data: NotificationData? = null)
}