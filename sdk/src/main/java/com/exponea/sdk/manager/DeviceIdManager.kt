package com.exponea.sdk.manager

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

internal object DeviceIdManager {
    private const val DEVICE_ID_PREFS_NAME = "DEVICE_ID_PREFS"
    private const val DEVICE_ID_KEY = "DEVICE_ID"

    @Volatile
    private var cachedDeviceId: String? = null

    fun getDeviceId(context: Context): String =
        cachedDeviceId ?: getOrGenerateDeviceId(context)

    @Synchronized
    private fun getOrGenerateDeviceId(context: Context): String {
        cachedDeviceId?.let {
            return it
        }

        val preferences = context.getSharedPreferences(DEVICE_ID_PREFS_NAME, Context.MODE_PRIVATE)
        val existingId = preferences.getString(DEVICE_ID_KEY, null)
        val id = existingId ?: UUID.randomUUID().toString().also {
            preferences.edit { putString(DEVICE_ID_KEY, it) }
        }
        cachedDeviceId = id
        return id
    }

    @Synchronized
    fun clear(context: Context) {
        cachedDeviceId = null
        val preferences = context.getSharedPreferences(DEVICE_ID_PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit { remove(DEVICE_ID_KEY) }
    }
}
