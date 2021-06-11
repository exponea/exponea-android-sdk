package com.exponea.sdk.repository

internal interface PushNotificationRepository {
    fun get(): Boolean
    fun set(boolean: Boolean)
    fun getExtraData(): Map<String, Any>?
    fun setExtraData(data: Map<String, Any>)
    fun clearExtraData()
}
