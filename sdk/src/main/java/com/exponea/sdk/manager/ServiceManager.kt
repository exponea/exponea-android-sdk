package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.services.OnIntegrationStoppedCallback

internal interface ServiceManager : OnIntegrationStoppedCallback {
    fun startPeriodicFlush(context: Context, flushPeriod: FlushPeriod)
    fun stopPeriodicFlush(context: Context)
    override fun onIntegrationStopped()
}
