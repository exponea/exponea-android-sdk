package com.exponea.sdk.view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.returnOnException

internal class InAppMessageDialogPresenter(context: Context) {
    private var presenting = false
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
        messageType: InAppMessageType,
        payload: InAppMessagePayload,
        image: Bitmap?,
        actionCallback: () -> Unit,
        dismissedCallback: () -> Unit
    ): InAppMessageView? = runCatching {
        if (presenting) {
            return null
        }
        val presenterActionCallback = {
            presenting = false
            actionCallback()
        }
        val presenterDismissedCallback = {
            presenting = false
            dismissedCallback()
        }
        val inAppMessageView = when (messageType) {
            InAppMessageType.MODAL, InAppMessageType.FULLSCREEN -> InAppMessageDialog(
                currentActivity ?: return null,
                messageType == InAppMessageType.FULLSCREEN,
                payload,
                image ?: return null,
                presenterActionCallback,
                presenterDismissedCallback
            )
            InAppMessageType.ALERT -> InAppMessageAlert(
                currentActivity ?: return null,
                payload,
                presenterActionCallback,
                presenterDismissedCallback
            )
            InAppMessageType.SLIDE_IN -> InAppMessageSlideIn(
                currentActivity ?: return null,
                payload,
                image ?: return null,
                presenterActionCallback,
                presenterDismissedCallback
            )
        }
        presenting = true
        try {
            inAppMessageView.show()
        } catch (e: Throwable) {
            Logger.w(this, "Showing in-app message failed $e.")
            presenting = false
            return null
        }
        return inAppMessageView
    }.returnOnException {
        presenting = false
        null
    }
}
