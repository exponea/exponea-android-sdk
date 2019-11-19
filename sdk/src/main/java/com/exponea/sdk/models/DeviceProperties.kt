package com.exponea.sdk.models

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.util.getAppVersion

internal data class DeviceProperties(
    val osName: String,
    val osVersion: String,
    val sdk: String,
    val sdkVersion: String,
    val deviceModel: String,
    val deviceType: String,
    val appVersion: String
) {
    companion object {
        private fun getDeviceType(context: Context): String {
            val deviceSize = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
            return if (deviceSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) "tablet" else "mobile"
        }
    }

    constructor(context: Context) : this(
        osName = Constants.DeviceInfo.osName,
        osVersion = Build.VERSION.RELEASE,
        sdk = Constants.DeviceInfo.sdk,
        sdkVersion = BuildConfig.VERSION_NAME,
        deviceModel = Build.MODEL,
        deviceType = getDeviceType(context),
        appVersion = context.getAppVersion(context)
    )

    fun toHashMap(): HashMap<String, Any> = hashMapOf(
        "os_name" to osName,
        "os_version" to osVersion,
        "sdk" to sdk,
        "sdk_version" to sdkVersion,
        "device_model" to deviceModel,
        "device_type" to deviceType,
        "app_version" to appVersion
    )
}
