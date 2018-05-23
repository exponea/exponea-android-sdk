package com.exponea.sdk.models

import android.app.NotificationManager

data class ExponeaConfiguration(
        // Default project token.
        var projectToken: String = "",
        // Map routes and project tokens to be send to Exponea API.
        var projectTokenRouteMap: HashMap<Route, MutableList<String>> = hashMapOf(),
        // Authorization http header.
        var authorization: String? = null,
        // Base url for http requests to Exponea API.
        var baseURL: String = Constants.Repository.baseURL,
        // Content type value to make http requests.
        var contentType: String = Constants.Repository.contentType,
        // Maximum retries value to flush data to api.
        var maxTries: Int = 10,
        // Timeout session value considered for app usage.
        var sessionTimeout: Double = 20.0,
        // Flag to control automatic tracking for In-App purchases
        var automaticPaymentTracking: Boolean = true,
        // Flag to control automatic session tracking
        var automaticSessionTracking: Boolean = true,
        // Flag to control if the App will handle push notifications automatically.
        var automaticPushNotification: Boolean = true,
        // Icon to be showed in push notifications.
        var pushIcon: Int? = null,
        // Channel name for push notifications. Only for API level 26+.
        var pushChannelName: String = "Exponea",
        // Channel description for push notifications. Only for API level 26+.
        var pushChannelDescription: String = "Notifications",
        // Channel ID for push notifications. Only for API level 26+.
        var pushChannelId: String = "0",
        // Notification importance for the notification channel. Only for API level 26+.
        var pushNotificationImportance: Int = NotificationManager.IMPORTANCE_DEFAULT
) {
}