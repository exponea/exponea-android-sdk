package com.exponea.sdk.view

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.exponea.sdk.R
import com.exponea.sdk.models.HtmlActionType
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessageButtonType.BROWSER
import com.exponea.sdk.models.InAppMessageButtonType.CANCEL
import com.exponea.sdk.models.InAppMessageButtonType.DEEPLINK
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.runOnMainThread

internal class InAppMessageWebview(
    private val activity: Activity,
    private val normalizedResult: HtmlNormalizer.NormalizedResult,
    private val onButtonClick: (InAppMessagePayloadButton) -> Unit,
    onDismiss: (Boolean, InAppMessagePayloadButton?) -> Unit,
    onError: (String) -> Unit
) : PopupWindow(
            LayoutInflater.from(activity).inflate(R.layout.in_app_message_webview, null, false),
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
            true),
            InAppMessageView {
    private var onDismiss: ((Boolean, InAppMessagePayloadButton?) -> Unit)? = onDismiss
    private var onError: ((String) -> Unit)? = onError
    private var webView: ExponeaWebView
    private var userInteraction = false

    override val isPresented: Boolean
        get() = isShowing

    init {
        webView = contentView.findViewById(R.id.content_html)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.setOnUrlCallback { url ->
            handleActionClick(url)
        }
        setOnDismissListener {
            if (!userInteraction) {
                this.onDismiss?.invoke(false, null)
            }
        }
        if (normalizedResult.valid && normalizedResult.html != null) {
            runOnMainThread {
                webView.loadData(normalizedResult.html!!)
            }
        } else {
            Logger.w(this, "[HTML] Message has invalid payload, canceling of message presentation")
            this.onError?.invoke("Invalid HTML or empty")
        }
    }

    internal fun handleActionClick(url: String?) {
        Logger.i(this@InAppMessageWebview, "[HTML] action for $url")
        val htmlAction = url?.let { normalizedResult.findActionInfoByUrl(it) }
        when (htmlAction?.actionType) {
            null -> {
                Logger.w(this, "[HTML] Unknown action URL: $url")
            }
            HtmlActionType.CLOSE -> {
                userInteraction = true
                this.onDismiss?.invoke(true, toPayloadButton(htmlAction))
            }
            else -> {
                userInteraction = true
                this.onButtonClick.invoke(toPayloadButton(htmlAction))
            }
        }
        dismiss()
    }

    private fun toPayloadButton(action: HtmlNormalizer.ActionInfo): InAppMessagePayloadButton {
        return InAppMessagePayloadButton(
            rawType = detectActionType(action.actionType).value,
            text = action.buttonText,
            link = action.actionUrl,
            fontUrl = "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
            sizing = "hug",
            radius = "12dp",
            margin = "20px 10px 15px 10px",
            textSize = "24px",
            lineHeight = "32px",
            padding = "20px 10px 15px 10px",
            textStyle = listOf("bold"),
            borderWeight = "0px",
            borderColor = "#00000000"
        )
    }

    private fun detectActionType(from: HtmlActionType): InAppMessageButtonType {
        return when (from) {
            HtmlActionType.DEEPLINK -> DEEPLINK
            HtmlActionType.BROWSER -> BROWSER
            HtmlActionType.CLOSE -> CANCEL
        }
    }

    override fun show() {
        Logger.i(this, "Showing webview")
        try {
            showAtLocation(
                    activity.window.decorView.rootView,
                    Gravity.CENTER,
                    0,
                    0
            )
        } catch (e: Exception) {
            Logger.e(this, "[InApp] Unable to show HTML in-app message", e)
            onError?.invoke("Invalid app foreground state")
        }
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            Logger.e(this, "[InApp] Dismissing HTML in-app message failed", e)
        }
    }
}
