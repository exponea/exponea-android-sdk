package com.exponea.sdk.view

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessageButtonType.BROWSER
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
        when {
            normalizedResult.isCloseAction(url) -> {
                userInteraction = true
                this.onDismiss?.invoke(true, toPayloadButton(url!!))
            }
            normalizedResult.isActionUrl(url) -> {
                userInteraction = true
                this.onButtonClick.invoke(toPayloadButton(url!!))
            }
            else -> Logger.w(this, "[HTML] Unknown action URL: $url")
        }
        dismiss()
    }

    private fun toPayloadButton(url: String): InAppMessagePayloadButton {
        return InAppMessagePayloadButton(
                buttonLink = url,
                buttonText = normalizedResult.findActionByUrl(url)?.buttonText,
                rawButtonType = detectActionType(url).value
        )
    }

    private fun detectActionType(url: String): InAppMessageButtonType {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return BROWSER
        } else {
            return DEEPLINK
        }
    }

    override fun show() {
        Logger.i(this, "Showing webview")
        showAtLocation(
                activity.window.decorView.rootView,
                Gravity.CENTER,
                0,
                0
        )
    }
}
