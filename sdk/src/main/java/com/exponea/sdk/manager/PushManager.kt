package com.exponea.sdk.manager

interface PushManager {
    val fcmToken: String

    fun trackFcmToken()
    fun trackDeliveredPush()
    fun trackClickedPush()
}