package com.exponea.sdk.repository

import com.exponea.sdk.Exponea
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger

internal class DeviceInitiatedRepositoryImpl(
    private val preferences: ExponeaPreferences
) : DeviceInitiatedRepository {
    private val KEY = "ExponeaDeviceInitiated"

    override fun get(): Boolean {
        if (Exponea.isStopped) {
            Logger.e(this, "Install flag not loaded, SDK is stopping")
            return false
        }
        return preferences.getBoolean(KEY, false)
    }

    override fun set(boolean: Boolean) {
        preferences.setBoolean(KEY, boolean)
    }

    override fun onIntegrationStopped() {
        set(false)
    }
}
