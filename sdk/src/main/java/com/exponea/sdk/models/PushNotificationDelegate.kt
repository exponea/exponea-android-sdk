package com.exponea.sdk.models

interface PushNotificationDelegate {
    fun onSilentPushNotificationReceived(notificationData: Map<String, Any>)
    fun onPushNotificationReceived(notificationData: Map<String, Any>)
    fun onPushNotificationOpened(
        action: ExponeaNotificationActionType,
        url: String?,
        notificationData: Map<String, Any>
    )
}
