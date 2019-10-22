package com.exponea.sdk.repository

internal interface PushNotificationRepository {
    fun get(): Boolean
    fun set(boolean: Boolean)
    fun getExtraData(): Map<String, String>?
    fun setExtraData(data: Map<String, String>)
    fun clearExtraData()
}
