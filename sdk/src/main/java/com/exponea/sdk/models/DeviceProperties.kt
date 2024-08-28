package com.exponea.sdk.models

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.util.getAppVersion
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

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

        fun isXiaomi(): Boolean {
            // Check if the device is manufactured by Xiaomi, Redmi, or POCO.
            val brand = Build.BRAND.lowercase()
            if (!setOf("xiaomi", "redmi", "poco").contains(brand)) return false
            // This property is present in both MIUI and HyperOS.
            val isMiui = !getProperty("ro.miui.ui.version.name").isNullOrBlank()
            return isMiui
        }

        private fun getProperty(property: String): String? {
            return try {
                Runtime.getRuntime().exec("getprop $property").inputStream.use { input ->
                    BufferedReader(InputStreamReader(input), 1024).readLine()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    constructor(context: Context) : this(
        osName = Constants.DeviceInfo.osName,
        osVersion = Build.VERSION.RELEASE,
        sdk = Constants.DeviceInfo.sdk,
        sdkVersion = BuildConfig.EXPONEA_VERSION_NAME,
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
