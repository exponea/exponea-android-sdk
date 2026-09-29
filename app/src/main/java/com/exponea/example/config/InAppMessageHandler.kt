package com.exponea.example.config

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.exponea.example.model.SdkSetupState
import com.exponea.example.status.SdkStatusStore
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButton
import com.exponea.sdk.models.InAppMessageCallback
import com.exponea.sdk.models.PropertiesList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object InAppMessageHandler {

    private val tag = this::class.simpleName

    fun create(context: Context): InAppMessageCallback = object : InAppMessageCallback {
        override var overrideDefaultBehavior = true
        override var trackActions = true

        override fun inAppMessageShown(message: InAppMessage, context: Context) {
            Log.i(tag, "In app message ${message.name} has been shown")
            if (message.name.contains("StopSDK")) {
                CoroutineScope(Dispatchers.Default).launch {
                    delay(4000)
                    stopIntegration()
                }
            }
        }

        override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {
            Log.e(tag, "Error occurred '$errorMessage' while showing in app message ${message?.name}")
        }

        override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, ctx: Context) {
            Log.i(tag, "In app message ${message.name} has been clicked: ${button.url}")
            if (messageIsForGdpr(message)) {
                handleGdprUserResponse(button)
            } else if (button.url != null) {
                openUrl(ctx, button)
            }
        }

        override fun inAppMessageCloseAction(
            message: InAppMessage,
            button: InAppMessageButton?,
            interaction: Boolean,
            context: Context
        ) {
            Log.i(tag, "In app message ${message.name} has been closed: ${button?.url}")
            if (messageIsForGdpr(message) && interaction) {
                stopIntegration()
            }
        }

        private fun handleGdprUserResponse(button: InAppMessageButton) {
            when (button.url) {
                "https://bloomreach.com/tracking/allow" -> {
                    Exponea.trackEvent(
                        eventType = "gdpr",
                        properties = PropertiesList(hashMapOf("status" to "allowed"))
                    )
                }
                "https://bloomreach.com/tracking/deny" -> stopIntegration()
            }
        }

        private fun messageIsForGdpr(message: InAppMessage): Boolean {
            return message.applyEventFilter("event_name", mapOf("property" to "gdpr"), null)
        }

        private fun openUrl(ctx: Context, button: InAppMessageButton) {
            try {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = Uri.parse(button.url)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                Log.e(tag, "Unable to open URL", e)
            }
        }

        private fun stopIntegration() {
            Exponea.stopIntegration {
                SdkSetupState.reset()
                SdkStatusStore.syncFromSdk()
            }
        }
    }
}
