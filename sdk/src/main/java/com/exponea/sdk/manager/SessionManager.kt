package com.exponea.sdk.manager

import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.util.OnForegroundStateListener
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.logOnException

internal abstract class SessionManager : OnForegroundStateListener, OnIntegrationStoppedCallback {

    abstract fun onSessionStart()

    abstract fun onSessionEnd()

    abstract fun startSessionListener()

    abstract fun stopSessionListener()

    abstract fun trackSessionEnd(timestamp: Double = currentTimeSeconds())

    abstract fun trackSessionStart(timestamp: Double = currentTimeSeconds())

    abstract fun reset()

    override fun onStateChanged(isForeground: Boolean) {
        runCatching {
            if (isForeground) {
                onSessionStart()
            } else {
                onSessionEnd()
            }
        }.logOnException()
    }

    abstract override fun onIntegrationStopped()
}
