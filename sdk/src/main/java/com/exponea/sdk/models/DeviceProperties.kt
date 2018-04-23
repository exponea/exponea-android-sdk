package com.exponea.sdk.models

import android.os.Build

data class DeviceProperties(
        var osName: String = Constants.DeviceInfo.osName,
        var osVersion: Int = Build.VERSION.SDK_INT,
        var sdk: String = Constants.DeviceInfo.sdk,
        var sdkVersion: String = Constants.DeviceInfo.sdkVersion,
        var deviceModel: String = Build.MODEL,
        var deviceType: String
)