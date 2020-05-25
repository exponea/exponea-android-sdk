package com.exponea.sdk.manager

internal interface PushNotificationSelfCheckManager {
    fun start()
    fun selfCheckPushReceived()
}
