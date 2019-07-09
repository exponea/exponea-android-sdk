package com.exponea.sdk.models

import android.app.NotificationManager

data class ExponeaConfiguration(
        /** Default project token. */
        var projectToken: String = "",
        /** Map event types and project tokens to be send to Exponea API. */
        var projectTokenRouteMap: HashMap<EventType, MutableList<String>> = hashMapOf(),
        /** Authorization http header. */
        var authorization: String? = null,
        /** Base url for http requests to Exponea API. */
        var baseURL: String = Constants.Repository.baseURL,
        /** Level of HTTP logging, default value is BODY. */
        var httpLoggingLevel: HttpLoggingLevel = HttpLoggingLevel.BODY,
        /** Content type value to make http requests. */
        var contentType: String = Constants.Repository.contentType,
        /** Maximum retries value to flush data to api. */
        var maxTries: Int = 10,
        /** Timeout session value considered for app usage. */
        var sessionTimeout: Double = 20.0,
        /** Flag to control automatic tracking for In-App purchases */
        var automaticPaymentTracking: Boolean = true,
        /** Flag to control automatic session tracking */
        var automaticSessionTracking: Boolean = true,
        /** Flag to control if the App will handle push notifications automatically. */
        var automaticPushNotification: Boolean = true,
        /** Icon to be showed in push notifications. */
        var pushIcon: Int? = null,
        /** Channel name for push notifications. Only for API level 26+. */
        var pushChannelName: String = "Exponea",
        /** Channel description for push notifications. Only for API level 26+. */
        var pushChannelDescription: String = "Notifications",
        /** Channel ID for push notifications. Only for API level 26+. */
        var pushChannelId: String = "0",
        /** Notification importance for the notification channel. Only for API level 26+. */
        var pushNotificationImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        /** A list of SKUs for automatic in-app purchases tracking*/
        var skuList: List<String> = arrayListOf(),
        /** A list of properties to be added to all tracking events */
        var defaultProperties: HashMap<String, Any> = hashMapOf(),
        /** How ofter the token is tracked */
        var tokenUpdateFrequency: TokenFrequency = TokenFrequency.DAILY
) {

    enum class HttpLoggingLevel {
        /** No logs. */
        NONE,
        /** Logs request and response lines. */
        BASIC,
        /** Logs request and response lines and their respective headers. */
        HEADERS,
        /** Logs request and response lines and their respective headers and bodies (if present). */
        BODY
    }

    enum class TokenFrequency {
        /** Tracked on the first launch or if the token changes */
        ON_TOKEN_CHANGE,
        /** Tracked every time the app is launched */
        EVERY_LAUNCH,
        /** Tracked once on days where the user opens the app */
        DAILY
    }

}