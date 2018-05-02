package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences

class PushNotificationRepositoryImpl(
        private val preferences: ExponeaPreferences
) : PushNotificationRepository {

    private val KEY = "ExponeaPushNotificationInitiated"

    override fun get(): Boolean {
        return preferences.getBoolean(KEY, false)
    }

    override fun set(boolean: Boolean) {
        preferences.setBoolean(KEY, boolean)
    }
}