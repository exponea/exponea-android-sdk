package com.exponea.sdk.view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isResumedActivity
import com.exponea.sdk.util.returnOnException
import java.lang.Exception

internal class InAppMessagePresenter(context: Context) {
    class PresentedMessage(
        val messageType: InAppMessageType,
        val payload: InAppMessagePayload,
        val image: Bitmap?,
        val timeout: Long?,
        val actionCallback: ((InAppMessagePayloadButton) -> Unit),
        val dismissedCallback: (() -> Unit),
        val failedCallback: (() -> Unit)
    )

    companion object {
        fun getView(
            activity: Activity,
            messageType: InAppMessageType,
            payload: InAppMessagePayload,
            image: Bitmap?,
            timeout: Long?,
            actionCallback: (InAppMessagePayloadButton) -> Unit,
            dismissedCallback: () -> Unit
        ): InAppMessageView? {
            var messageTimeout = timeout
            val view = when (messageType) {
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
                InAppMessageType.SLIDE_IN -> {
                    // slide-in message has 4 second auto-dismiss default
                    if (timeout == null) messageTimeout = 4000
                    InAppMessageSlideIn(
                        activity,
                        payload,
                        image ?: return null,
                        actionCallback,
                        dismissedCallback
                    )
                }
            }
            if (messageTimeout != null) {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
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
                    },
                    messageTimeout
                )
            }
            return view
        }
    }

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
        timeout: Long?,
        actionCallback: (Activity, InAppMessagePayloadButton) -> Unit,
        dismissedCallback: (Activity) -> Unit
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
            presentedMessage = null
            actionCallback(activity, button)
        }
        val presenterDismissedCallback = {
            presentedMessage = null
            dismissedCallback(activity)
        }
        val failedCallback = {
            presentedMessage = null
        }
        presentedMessage =
            PresentedMessage(
                messageType,
                payload,
                image,
                timeout,
                presenterActionCallback,
                presenterDismissedCallback,
                failedCallback
            )

        when (messageType) {
            InAppMessageType.MODAL, InAppMessageType.FULLSCREEN, InAppMessageType.ALERT -> {
                val intent = Intent(activity, InAppMessageActivity::class.java)
                activity.startActivity(intent)
            }
            InAppMessageType.SLIDE_IN -> {
                getView(
                    activity,
                    messageType,
                    payload,
                    image,
                    timeout,
                    presenterActionCallback,
                    presenterDismissedCallback
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
