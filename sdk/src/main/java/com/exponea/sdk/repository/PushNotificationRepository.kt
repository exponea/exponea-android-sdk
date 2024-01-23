package com.exponea.sdk.repository

internal interface PushNotificationRepository {
    fun getExtraData(): Map<String, Any>?
    fun setExtraData(data: Map<String, Any>)
    fun clearExtraData()
}
