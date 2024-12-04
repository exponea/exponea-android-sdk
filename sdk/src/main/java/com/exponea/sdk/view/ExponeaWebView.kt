package com.exponea.sdk.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.util.AttributeSet
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import java.util.concurrent.atomic.AtomicInteger

public class ExponeaWebView : WebView {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private var onUrlClickCallback: ((String) -> Unit)? = null
    internal var onPageLoadedCallback: (() -> Unit)? = null
    private val loadedHtmlCrc = AtomicInteger()

    private fun init() {
        applyAntiXssSetup()
        logOnError()
        registerUrlHandler()
    }

    private fun registerUrlHandler() {
        webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Logger.d(this, "[HTML] URL invoked from Intenal webview $url")
                url?.let { urlAction ->
                    onUrlClickCallback?.invoke(urlAction)
                }
                // stop URL loading, we are using only single-page HTML content
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Logger.d(this, "[HTML] Web page has been loaded")
                onPageLoadedCallback?.invoke()
            }
        }
    }

    private fun logOnError() {
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Logger.d(this, "[HTML] ${message.message()} -- From line ${message.lineNumber()}")
                return true
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyAntiXssSetup() {
        Exponea.getComponent()?.let {
            val allowWebViewCookies = it.exponeaConfiguration.allowWebViewCookies
            CookieManager.getInstance().setAcceptCookie(allowWebViewCookies)
            if (VERSION.SDK_INT >= LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, allowWebViewCookies)
            }
        }
        this.settings.apply {
            setGeolocationEnabled(false)
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            allowContentAccess = false
            allowFileAccess = false
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
        }
    }

    public fun loadData(html: String) {
        val htmlCrc = html.hashCode()
        if (loadedHtmlCrc.getAndSet(htmlCrc) == htmlCrc) {
            Logger.v(this, "[HTML] WebView wants to load same HTML content, force-refresh required")
            loadDataCompat("")
        }
        loadDataCompat(html)
    }

    private fun loadDataCompat(html: String) {
        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } else {
            loadData(
                Base64.encodeToString(html.toByteArray(), Base64.DEFAULT),
                "text/html",
                "base64"
            )
        }
    }

    fun setOnUrlCallback(callback: ((String) -> Unit)?) {
        onUrlClickCallback = callback
    }

    fun setOnPageLoadedCallback(callback: (() -> Unit)?) {
        onPageLoadedCallback = callback
    }
}
