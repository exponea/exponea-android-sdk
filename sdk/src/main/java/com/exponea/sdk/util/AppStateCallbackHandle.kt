package com.exponea.sdk.util

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.exponea.sdk.services.OnIntegrationStoppedCallback

internal class AppStateCallbackHandle(
    context: Context,
    private val onOpen: () -> Unit,
    private val onClosed: () -> Unit
) : OnIntegrationStoppedCallback {

    private val application = context.applicationContext as Application

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        private var activityCount: Int = 0

        override fun onActivityResumed(activity: Activity) {
            runCatching {
                onOpen()
            }.logOnException()
            activityCount++
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityPaused(activity: Activity) {
            activityCount--
            if (activityCount <= 0) {
                runCatching {
                    onClosed()
                }.logOnException()
            }
        }
    }

    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onLowMemory() {}

        override fun onConfigurationChanged(newConfig: Configuration) {}

        override fun onTrimMemory(level: Int) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                runCatching {
                    onClosed()
                }.logOnException()
            }
        }
    }

    fun start() {
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        application.registerComponentCallbacks(componentCallbacks)
    }

    override fun onIntegrationStopped() {
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        application.unregisterComponentCallbacks(componentCallbacks)
    }
}
