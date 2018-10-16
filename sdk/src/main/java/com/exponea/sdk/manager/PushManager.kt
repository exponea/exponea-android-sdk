package com.exponea.sdk.manager

import com.exponea.sdk.models.NotificationData

interface PushManager {
    val fcmToken: String?

    fun trackFcmToken()
    fun trackDeliveredPush(data: NotificationData? = null)
    fun trackClickedPush(data: NotificationData? = null)
}