package com.exponea.sdk.view

import android.app.Activity
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessageButtonType.BROWSER
import com.exponea.sdk.models.InAppMessageButtonType.DEEPLINK
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.URLUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class InAppMessageWebview(
    private val activity: Activity,
    private val normalizedResult: HtmlNormalizer.NormalizedResult,
    private val onButtonClick: (InAppMessagePayloadButton) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) : PopupWindow(
            LayoutInflater.from(activity).inflate(R.layout.in_app_message_webview, null, false),
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT,
            true),
            InAppMessageView {
    private var onDismiss: (() -> Unit)? = onDismiss
    private var onError: ((String) -> Unit)? = onError
    private var webView: WebView

    override val isPresented: Boolean
        get() = isShowing

    init {
        webView = contentView.findViewById(R.id.content_html)
        webView.setBackgroundColor(Color.TRANSPARENT)
        applyAntiXssSetup(webView)
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                handleActionClick(url)
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Logger.d(this@InAppMessageWebview, "[HTML] ${message.message()} -- From line ${message.lineNumber()}")
                return true
            }
        }
        setOnDismissListener { this.onDismiss?.invoke() }
        if (normalizedResult.valid && normalizedResult.html != null) {
            GlobalScope.launch(Dispatchers.Main) {
                if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                    webView.loadDataWithBaseURL(null, normalizedResult.html!!, "text/html", "UTF-8", null)
                } else {
                    webView.loadData(
                        Base64.encodeToString(normalizedResult.html!!.toByteArray(), Base64.DEFAULT),
                        "text/html",
                        "base64"
                    )
                }
            }
        } else {
            Logger.w(this, "[HTML] Message has invalid payload, canceling of message presentation")
            this.onError?.invoke("Invalid HTML or empty")
        }
    }

    @Suppress("DEPRECATION")
    private fun applyAntiXssSetup(webView: WebView?) {
        CookieManager.getInstance().setAcceptCookie(false)
        webView?.settings?.apply {
            setGeolocationEnabled(false)
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            allowContentAccess = false
            allowFileAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            saveFormData = false
            savePassword = false
            javaScriptEnabled = false
            javaScriptCanOpenWindowsAutomatically = false
            blockNetworkImage = true
            blockNetworkLoads = true
            databaseEnabled = false
            domStorageEnabled = false
            loadWithOverviewMode = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            setAppCacheEnabled(false)
        }
    }

    private fun handleActionClick(url: String?) {
        Logger.i(this@InAppMessageWebview, "[HTML] action for $url")
        when {
            isCloseAction(url) -> this.onDismiss?.invoke()
            isActionUrl(url) -> this.onButtonClick.invoke(toPayloadButton(url!!))
            else -> Logger.w(this, "[HTML] Unknown action URL: $url")
        }
        dismiss()
    }

    private fun toPayloadButton(url: String): InAppMessagePayloadButton {
        return InAppMessagePayloadButton(
                buttonLink = url,
                buttonText = findActionByUrl(url)?.buttonText ?: "Unknown",
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

    private fun findActionByUrl(url: String): HtmlNormalizer.ActionInfo? {
        return this.normalizedResult.actions?.find { URLUtils.areEqualAsURLs(it.actionUrl, url) }
    }

    private fun isActionUrl(url: String?): Boolean {
        return url != null && !isCloseAction(url) && findActionByUrl(url) != null
    }

    private fun isCloseAction(url: String?): Boolean {
        return url?.equals(normalizedResult.closeActionUrl) ?: false
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
