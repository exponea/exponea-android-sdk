package com.exponea.sdk.manager

internal interface DeviceManager {
    fun isTablet(): Boolean
    fun getDeviceType(): String
}

