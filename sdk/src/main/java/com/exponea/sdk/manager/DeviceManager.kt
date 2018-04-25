package com.exponea.sdk.manager

import android.content.Context
import android.content.res.Configuration
import com.exponea.sdk.util.Logger

class DeviceManager(private val context: Context) {
    fun isTablet(): Boolean {
        val deviceSize = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val isDeviceLarge = deviceSize >= Configuration.SCREENLAYOUT_SIZE_LARGE

        return if (isDeviceLarge) {
            Logger.d(this, "Detect tablet")
            true
        } else {
            Logger.d(this, "Detect mobile")
            false
        }
    }

    fun getDeviceType(): String {
        return if (isTablet()) {
            "tablet"
        } else {
            "mobile"
        }
    }
}