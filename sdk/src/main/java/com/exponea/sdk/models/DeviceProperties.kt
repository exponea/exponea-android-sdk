package com.exponea.sdk.models

import android.os.Build

data class DeviceProperties(
        var osName: String = Constants.DeviceInfo.osName,
        var osVersion: String = Build.VERSION.RELEASE,
        var sdk: String = Constants.DeviceInfo.sdk,
        var sdkVersion: String = Constants.DeviceInfo.sdkVersion,
        var deviceModel: String = Build.MODEL,
        var deviceType: String
) {
    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
                Pair("os_name", osName),
                Pair("os_version", osVersion),
                Pair("sdk", sdk),
                Pair("sdk_version", sdkVersion),
                Pair("device_model", deviceModel),
                Pair("device_type", deviceType)
        )
    }
}