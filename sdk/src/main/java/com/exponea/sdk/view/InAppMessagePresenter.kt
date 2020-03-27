package com.exponea.sdk.view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.util.returnOnException

internal class InAppMessagePresenter(val context: Context) {
    class PresentedMessage(
        val messageType: InAppMessageType,
        val payload: InAppMessagePayload,
        val image: Bitmap?,
        val actionCallback: ((InAppMessagePayloadButton) -> Unit),
        val dismissedCallback: (() -> Unit)
    )

    companion object {
        fun getView(
            activity: Activity,
            messageType: InAppMessageType,
            payload: InAppMessagePayload,
            image: Bitmap?,
            actionCallback: (InAppMessagePayloadButton) -> Unit,
            dismissedCallback: () -> Unit
        ): InAppMessageView? {
            return when (messageType) {
                InAppMessageType.MODAL, InAppMessageType.FULLSCREEN -> InAppMessageDialog(
                    activity,
                    messageType == InAppMessageType.FULLSCREEN,
                    payload,
                    image ?: return null,
                    actionCallback,
                    dismissedCallback
                )
                InAppMessageType.ALERT -> InAppMessageAlert(
                    activity,
                    payload,
                    actionCallback,
                    dismissedCallback
                )
                InAppMessageType.SLIDE_IN -> InAppMessageSlideIn(
                    activity,
                    payload,
                    image ?: return null,
                    actionCallback,
                    dismissedCallback
                )
            }
        }
    }

    var presentedMessage: PresentedMessage? = null
        private set

    private var currentActivity: Activity? = null
    /**
     * In order to display the message, we need context of current activity / know that the app is foregrounded.
     * We have application context from SDK init, we'll hook into app lifecycle.
     */
    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
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
        actionCallback: (InAppMessagePayloadButton) -> Unit,
        dismissedCallback: () -> Unit
    ): PresentedMessage? = runCatching {
        val activity = currentActivity
        if (presentedMessage != null || activity == null) {
            return null
        }
        val presenterActionCallback = { button: InAppMessagePayloadButton ->
            presentedMessage = null
            actionCallback(button)
        }
        val presenterDismissedCallback = {
            presentedMessage = null
            dismissedCallback()
        }
        presentedMessage =
            PresentedMessage(messageType, payload, image, presenterActionCallback, presenterDismissedCallback)

        when (messageType) {
            InAppMessageType.MODAL, InAppMessageType.FULLSCREEN, InAppMessageType.ALERT -> {
                val intent = Intent(context, InAppMessageActivity::class.java)
                context.startActivity(intent)
            }
            InAppMessageType.SLIDE_IN -> {
                getView(
                    activity,
                    messageType,
                    payload,
                    image,
                    presenterActionCallback,
                    presenterDismissedCallback
                )?.show()
            }
        }
        return presentedMessage
    }.returnOnException {
        presentedMessage = null
        null
    }
}
