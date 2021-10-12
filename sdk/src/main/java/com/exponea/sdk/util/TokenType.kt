package com.exponea.sdk.util

import com.exponea.sdk.models.Constants

internal enum class TokenType(val apiProperty: String, val selfCheckProperty: String) {
    FCM(Constants.PushNotif.fcmTokenProperty, Constants.PushNotif.fcmSelfCheckPlatformProperty),
    HMS(Constants.PushNotif.hmsTokenProperty, Constants.PushNotif.hmsSelfCheckPlatformProperty)
}
