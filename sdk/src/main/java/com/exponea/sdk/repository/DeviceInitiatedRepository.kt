package com.exponea.sdk.repository

interface DeviceInitiatedRepository {
    fun get(): Boolean
    fun set(boolean: Boolean)
}