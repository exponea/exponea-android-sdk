package com.exponea.sdk.view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.models.InAppMessageUiPayload
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.util.isResumedActivity
import com.exponea.sdk.util.returnOnException
import com.exponea.sdk.util.runOnMainThread

internal class InAppMessagePresenter(
    internal val context: Context,
    internal val drawableCache: DrawableCache
) : OnIntegrationStoppedCallback {
    class PresentedMessage(
        val messageType: InAppMessageType,
        val payload: InAppMessagePayload?,
        val payloadUi: InAppMessageUiPayload?,
        val payloadHtml: HtmlNormalizer.NormalizedResult?,
        val timeout: Long?,
        val actionCallback: ((InAppMessagePayloadButton) -> Unit),
        val dismissedCallback: ((Boolean, InAppMessagePayloadButton?) -> Unit),
        val failedCallback: ((String) -> Unit)
    )

    enum class InAppMessageViewType {
        MODAL,
        FULLSCREEN,
        ALERT,
        SLIDE_IN,
        FREEFORM,
        RICHSTYLE_MODAL,
        RICHSTYLE_FULLSCREEN,
        RICHSTYLE_SLIDE_IN
    }

    companion object {
        const val SLIDE_IN_DEFAULT_TIMEOUT = 4000L
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
        dataPayload: InAppMessagePayload?,
        uiPayload: InAppMessageUiPayload?,
        payloadHtml: HtmlNormalizer.NormalizedResult?,
        timeout: Long?,
        actionCallback: (InAppMessagePayloadButton) -> Unit,
        dismissedCallback: (Boolean, InAppMessagePayloadButton?) -> Unit,
        errorCallback: (String) -> Unit
    ): InAppMessageView {
        val messageTimeout = if (messageType == InAppMessageType.SLIDE_IN && timeout == null) {
            // slide-in message has 4 second auto-dismiss default
            SLIDE_IN_DEFAULT_TIMEOUT
        } else {
            timeout
        }
        val viewType = chooseViewType(messageType, uiPayload != null)
        val view = when (viewType) {
            InAppMessageViewType.RICHSTYLE_MODAL, InAppMessageViewType.RICHSTYLE_FULLSCREEN -> {
                InAppMessageRichstyleDialog(
                    activity,
                    messageType == InAppMessageType.FULLSCREEN,
                    uiPayload!!,
                    actionCallback,
                    dismissedCallback,
                    errorCallback
                )
            }
            InAppMessageViewType.MODAL, InAppMessageViewType.FULLSCREEN -> {
                InAppMessageDialog(
                    activity,
                    messageType == InAppMessageType.FULLSCREEN,
                    dataPayload!!,
                    drawableCache,
                    actionCallback,
                    dismissedCallback,
                    errorCallback
                )
            }
            InAppMessageViewType.ALERT -> InAppMessageAlert(
                activity,
                dataPayload!!,
                actionCallback,
                dismissedCallback,
                errorCallback
            )
            InAppMessageViewType.RICHSTYLE_SLIDE_IN -> {
                InAppMessageRichstyleSlideIn(
                    activity,
                    uiPayload!!,
                    actionCallback,
                    dismissedCallback,
                    errorCallback
                )
            }
            InAppMessageViewType.SLIDE_IN -> {
                InAppMessageSlideIn(
                    activity,
                    dataPayload!!,
                    drawableCache,
                    actionCallback,
                    dismissedCallback,
                    errorCallback
                )
            }
            InAppMessageViewType.FREEFORM -> InAppMessageWebview(
                activity,
                payloadHtml!!,
                actionCallback,
                dismissedCallback,
                errorCallback
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

    private fun chooseViewType(messageType: InAppMessageType, richStyled: Boolean): InAppMessageViewType {
        return when (messageType) {
            InAppMessageType.MODAL ->
                if (richStyled) InAppMessageViewType.RICHSTYLE_MODAL else InAppMessageViewType.MODAL
            InAppMessageType.ALERT ->
                InAppMessageViewType.ALERT
            InAppMessageType.FULLSCREEN ->
                if (richStyled) InAppMessageViewType.RICHSTYLE_FULLSCREEN else InAppMessageViewType.FULLSCREEN
            InAppMessageType.SLIDE_IN ->
                if (richStyled) InAppMessageViewType.RICHSTYLE_SLIDE_IN else InAppMessageViewType.SLIDE_IN
            InAppMessageType.FREEFORM ->
                InAppMessageViewType.FREEFORM
        }
    }

    fun show(
        messageType: InAppMessageType,
        payload: InAppMessagePayload?,
        uiPayload: InAppMessageUiPayload?,
        payloadHtml: HtmlNormalizer.NormalizedResult?,
        timeout: Long?,
        actionCallback: (Activity, InAppMessagePayloadButton) -> Unit,
        dismissedCallback: (Activity, Boolean, InAppMessagePayloadButton?) -> Unit,
        failedCallback: (String) -> Unit
    ): PresentedMessage? = runCatching {
        Logger.i(this, "Attempting to present in-app message.")
        if (isPresenting()) {
            Logger.i(this, "Already presenting another in-app message.")
            // error track is not expected for this case
            return null
        }
        if (Exponea.isStopped) {
            Logger.e(this, "In-app UI is unavailable, SDK is stopping")
            return null
        }
        val presenterFailedCallback = { error: String ->
            Logger.i(this, "InApp got error $error")
            presentedMessage = null
            failedCallback(error)
        }
        val activity = currentActivity
        if (activity == null) {
            Logger.w(this, "No activity available to present in-app message.")
            presenterFailedCallback("No active activity")
            return null
        }
        val presenterActionCallback = { button: InAppMessagePayloadButton ->
            Logger.i(this, "InApp action clicked ${button.link}")
            presentedMessage = null
            actionCallback(activity, button)
        }
        val presenterDismissedCallback = { userInteraction: Boolean, cancelButton: InAppMessagePayloadButton? ->
            Logger.i(this, "InApp dismissed by user: ${if (userInteraction) "true" else "false"}")
            presentedMessage = null
            dismissedCallback(activity, userInteraction, cancelButton)
        }
        presentedMessage =
            PresentedMessage(
                messageType,
                payload,
                uiPayload,
                payloadHtml,
                timeout,
                presenterActionCallback,
                presenterDismissedCallback,
                presenterFailedCallback
            )
        ensureOnMainThread {
            runCatching {
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
                            uiPayload,
                            payloadHtml,
                            timeout,
                            presenterActionCallback,
                            presenterDismissedCallback,
                            presenterFailedCallback
                        ).show()
                    }
                }
            }.returnOnException {
                Logger.w(this, "Showing of in-app message failed. $it")
                presentedMessage = null
            }
        }
        Logger.i(this, "In-app message presented.")
        return presentedMessage
    }.returnOnException {
        Logger.w(this, "Presenting in-app message failed. $it")
        presentedMessage = null
        null
    }

    fun isPresenting(): Boolean {
        return presentedMessage != null
    }

    override fun onIntegrationStopped() {
        presentedMessage = null
    }
}
