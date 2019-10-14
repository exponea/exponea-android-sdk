package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences

class DeviceInitiatedRepositoryImpl(private val preferences: ExponeaPreferences) :
        DeviceInitiatedRepository {
    private val KEY = "ExponeaDeviceInitiated"

    override fun get(): Boolean {
        return preferences.getBoolean(KEY, false)
    }

    override fun set(boolean: Boolean) {
        preferences.setBoolean(KEY, boolean)
    }
}