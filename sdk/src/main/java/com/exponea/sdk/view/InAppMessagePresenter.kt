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
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isResumedActivity
import com.exponea.sdk.util.returnOnException
import com.exponea.sdk.util.runOnMainThread

internal class InAppMessagePresenter(
    context: Context,
    private var bitmapCache: InAppMessageBitmapCache
) {
    class PresentedMessage(
        val messageType: InAppMessageType,
        val payload: InAppMessagePayload?,
        val payloadHtml: HtmlNormalizer.NormalizedResult?,
        val timeout: Long?,
        val actionCallback: ((InAppMessagePayloadButton) -> Unit),
        val dismissedCallback: ((Boolean) -> Unit),
        val failedCallback: ((String) -> Unit)
    )

    var presentedMessage: PresentedMessage? = null
        private set

    private var currentActivity: Activity? = null
    /**
     * In order to display the message, we need context of current activity / know that the app is foregrounded.
     * We have application context from SDK init, we'll hook into app lifecycle.
     * If the SDK was initialized with resumed activity, we'll start using that activity.
     */
    init {
        if (context.isResumedActivity()) {
            currentActivity = context as? Activity
        }
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity) {
                    if (activity == currentActivity) currentActivity = null
                }
                override fun onActivityResumed(activity: Activity) {
                    currentActivity = activity
                }
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityDestroyed(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            }
        )
    }

    fun getView(
        activity: Activity,
        messageType: InAppMessageType,
        payload: InAppMessagePayload?,
        payloadHtml: HtmlNormalizer.NormalizedResult?,
        timeout: Long?,
        actionCallback: (InAppMessagePayloadButton) -> Unit,
        dismissedCallback: (Boolean) -> Unit,
        errorCallback: (String) -> Unit
    ): InAppMessageView? {
        var messageTimeout = timeout
        val view = when (messageType) {
            InAppMessageType.MODAL, InAppMessageType.FULLSCREEN -> InAppMessageDialog(
                    activity,
                    messageType == InAppMessageType.FULLSCREEN,
                    payload!!,
                    loadImageByPayload(payload) ?: return null,
                    actionCallback,
                    dismissedCallback
            )
            InAppMessageType.ALERT -> InAppMessageAlert(
                    activity,
                    payload!!,
                    actionCallback,
                    dismissedCallback
            )
            InAppMessageType.SLIDE_IN -> {
                // slide-in message has 4 second auto-dismiss default
                if (timeout == null) messageTimeout = 4000
                InAppMessageSlideIn(
                        activity,
                        payload!!,
                        loadImageByPayload(payload) ?: return null,
                        actionCallback,
                        dismissedCallback
                )
            }
            InAppMessageType.FREEFORM -> InAppMessageWebview(
                    activity, payloadHtml!!, actionCallback, dismissedCallback, errorCallback
            )
        }
        if (messageTimeout != null) {
            runOnMainThread(messageTimeout) {
                if (view.isPresented) {
                    try {
                        view.dismiss()
                    } catch (ex: Exception) {
                        Logger.i(
                            this,
                            "InAppMessageActivity is probably already destroyed," +
                                " skipping dialog dismiss")
                    }
                }
            }
        }
        return view
    }

    private fun loadImageByPayload(payload: InAppMessagePayload): Bitmap? {
        return payload.imageUrl?.let { bitmapCache.get(it) }
    }

    fun show(
        messageType: InAppMessageType,
        payload: InAppMessagePayload?,
        payloadHtml: HtmlNormalizer.NormalizedResult?,
        timeout: Long?,
        actionCallback: (Activity, InAppMessagePayloadButton) -> Unit,
        dismissedCallback: (Activity, Boolean) -> Unit,
        failedCallback: (String) -> Unit
    ): PresentedMessage? = runCatching {
        Logger.i(this, "Attempting to present in-app message.")
        val activity = currentActivity
        if (presentedMessage != null) {
            Logger.i(this, "Already presenting another in-app message.")
            return null
        }
        if (activity == null) {
            Logger.w(this, "No activity available to present in-app message.")
            return null
        }
        val presenterActionCallback = { button: InAppMessagePayloadButton ->
            Logger.i(this, "InApp action clicked ${button.buttonLink}")
            presentedMessage = null
            actionCallback(activity, button)
        }
        val presenterDismissedCallback = { userInteraction: Boolean ->
            Logger.i(this, "InApp dismissed by user: ${if (userInteraction) "true" else "false"}")
            presentedMessage = null
            dismissedCallback(activity, userInteraction)
        }
        val presenterFailedCallback = { error: String ->
            Logger.i(this, "InApp got error $error")
            presentedMessage = null
            failedCallback(error)
        }
        presentedMessage =
            PresentedMessage(
                messageType,
                payload,
                payloadHtml,
                timeout,
                presenterActionCallback,
                presenterDismissedCallback,
                presenterFailedCallback
            )

        when (messageType) {
            InAppMessageType.MODAL, InAppMessageType.FULLSCREEN, InAppMessageType.ALERT -> {
                val intent = Intent(activity, InAppMessageActivity::class.java)
                activity.startActivity(intent)
            }
            InAppMessageType.SLIDE_IN, InAppMessageType.FREEFORM -> {
                getView(
                    activity,
                    messageType,
                    payload,
                    payloadHtml,
                    timeout,
                    presenterActionCallback,
                    presenterDismissedCallback,
                    presenterFailedCallback
                )?.show()
            }
        }
        Logger.i(this, "In-app message presented.")
        return presentedMessage
    }.returnOnException {
        Logger.w(this, "Presenting in-app message failed. $it")
        presentedMessage = null
        null
    }
}
