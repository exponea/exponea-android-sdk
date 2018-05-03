package com.exponea.sdk.repository

interface PushNotificationRepository {
    fun get(): Boolean
    fun set(boolean: Boolean)
}