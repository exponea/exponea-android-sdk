package com.exponea.sdk.util

import android.app.Activity
import android.app.Application
import android.os.Bundle

abstract class SessionListener : Application.ActivityLifecycleCallbacks{

    abstract fun onSessionEnded()
    abstract fun onSessionStarted()

    override fun onActivityPaused(activity: Activity?) {
        onSessionEnded()
    }

    override fun onActivityResumed(activity: Activity?) {
        onSessionStarted()
    }
    override fun onActivityStarted(activity: Activity?) {}
    override fun onActivityDestroyed(activity: Activity?) {}
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
    override fun onActivityStopped(activity: Activity?) {}
    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
}