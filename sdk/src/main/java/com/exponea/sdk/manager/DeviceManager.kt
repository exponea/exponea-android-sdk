package com.exponea.sdk.manager

interface DeviceManager {
    fun isTablet(): Boolean
    fun getDeviceType(): String
}

