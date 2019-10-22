package com.exponea.sdk.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.currentTimeSeconds

internal abstract class SessionManager : Application.ActivityLifecycleCallbacks {

    abstract fun onSessionStart()

    abstract fun onSessionEnd()

    abstract fun startSessionListener()

    abstract fun stopSessionListener()

    abstract fun trackSessionEnd(timestamp: Double = currentTimeSeconds())

    abstract fun trackSessionStart(timestamp: Double = currentTimeSeconds())

    abstract fun reset()

    override fun onActivityPaused(activity: Activity?) {
        onSessionEnd()
    }

    override fun onActivityResumed(activity: Activity?) {
        onSessionStart()
        Exponea.component.campaignRepository.clear()
    }

    override fun onActivityStarted(activity: Activity?) {}
    override fun onActivityDestroyed(activity: Activity?) {}
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
    override fun onActivityStopped(activity: Activity?) {}
    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

}
