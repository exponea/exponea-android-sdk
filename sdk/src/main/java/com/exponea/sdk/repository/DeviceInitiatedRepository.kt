package com.exponea.sdk.repository

import com.exponea.sdk.services.OnIntegrationStoppedCallback

internal interface DeviceInitiatedRepository : OnIntegrationStoppedCallback {
    fun get(): Boolean
    fun set(boolean: Boolean)
    override fun onIntegrationStopped()
}
