package com.exponea.sdk.manager

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.exponea.sdk.util.Logger

class DeviceManager(
        private val context: Context
) {
    fun isTablet(): Boolean {
        val device_large = context.resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE

        if (device_large) {
            Logger.d(this, "Detect tablet")
            return true
        } else {
            Logger.d(this, "Detect mobile")
            return false
        }
    }
}