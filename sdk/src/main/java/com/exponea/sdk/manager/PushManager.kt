package com.exponea.sdk.manager

import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData

interface PushManager {

    val fcmToken: String?
    val lastTrackDateInMilliseconds: Long?

    fun trackFcmToken(token: String? = null)
    fun trackDeliveredPush(data: NotificationData? = null)
    fun trackClickedPush(data: NotificationData? = null, action: NotificationAction? = null)

}