package com.exponea.sdk.view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.util.returnOnException

internal class InAppMessageDialogPresenter(context: Context) {
    private var currentActivity: Activity? = null

    /**
     * In order to display dialog, we need context of current activity.
     * We have application context from SDK ini, we'll hook into app lifecycle.
     */
    init {
        (context as Application).registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity?) {
                    if (activity == currentActivity) currentActivity = null
                }
                override fun onActivityResumed(activity: Activity?) {
                    currentActivity = activity
                }
                override fun onActivityStarted(activity: Activity?) {}
                override fun onActivityDestroyed(activity: Activity?) {}
                override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
                override fun onActivityStopped(activity: Activity?) {}
                override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
            }
        )
    }

    fun show(
        payload: InAppMessagePayload,
        image: Bitmap,
        actionCallback: () -> Unit,
        dismissedCallback: () -> Unit
    ): Boolean = runCatching {
        val dialog = InAppMessageDialog(
            currentActivity ?: return false,
            payload,
            image,
            actionCallback,
            dismissedCallback
        )
        dialog.show()
        return true
    }.returnOnException { false }
}
