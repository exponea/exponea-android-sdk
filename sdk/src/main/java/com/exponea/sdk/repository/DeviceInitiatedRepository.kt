package com.exponea.sdk.repository

internal interface DeviceInitiatedRepository {
    fun get(): Boolean
    fun set(boolean: Boolean)
}
