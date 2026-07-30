package com.exponea.sdk.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.M
import android.util.AttributeSet
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.LocalResourceUrlMapper
import com.exponea.sdk.util.Logger
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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

    private companion object {
        private const val PAGE_RENDER_FALLBACK_TIMEOUT = 200L
        private const val PAGE_RENDER_SOURCE_VISUAL_STATE = "webview_visual_state"
        private const val PAGE_RENDER_SOURCE_POST = "webview_post"
        private const val PAGE_RENDER_SOURCE_TIMEOUT = "webview_timeout"
    }

    private var onUrlClickCallback: ((String) -> Unit)? = null
    internal var onPageLoadedCallback: ((String) -> Unit)? = null
    private val loadedHtmlCrc = AtomicInteger()
    private val pageRenderRequestCounter = AtomicInteger()
    private val pendingPageRenderRequest = AtomicReference<Int?>(null)
    @Volatile private var skipNextPageLoad = false

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
                if (skipNextPageLoad) {
                    skipNextPageLoad = false
                    Logger.v(this, "[HTML] Blank reset page loaded, skipping content-ready callback")
                    return
                }
                notifyPageRendered()
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return request?.url?.toString()?.let { url ->
                    createResourceResponse(url)
                } ?: super.shouldInterceptRequest(view, request)
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
            blockNetworkImage = false
            blockNetworkLoads = false
            databaseEnabled = false
            domStorageEnabled = false
            loadWithOverviewMode = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
    }

    public fun loadData(html: String) {
        val htmlCrc = html.hashCode()
        if (loadedHtmlCrc.getAndSet(htmlCrc) == htmlCrc) {
            Logger.v(this, "[HTML] WebView wants to load same HTML content, skipping redundant reload")
            return
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

    fun setOnPageLoadedCallback(callback: ((String) -> Unit)?) {
        onPageLoadedCallback = callback
    }

    private fun notifyPageRendered() {
        val requestId = pageRenderRequestCounter.incrementAndGet()
        pendingPageRenderRequest.set(requestId)
        postDelayed({
            completePageRenderRequest(requestId, PAGE_RENDER_SOURCE_TIMEOUT)
        }, PAGE_RENDER_FALLBACK_TIMEOUT)
        if (VERSION.SDK_INT >= M) {
            postVisualStateCallback(requestId.toLong(), object : VisualStateCallback() {
                override fun onComplete(requestId: Long) {
                    completePageRenderRequest(requestId.toInt(), PAGE_RENDER_SOURCE_VISUAL_STATE)
                }
            })
        } else {
            post {
                completePageRenderRequest(requestId, PAGE_RENDER_SOURCE_POST)
            }
        }
    }

    private fun completePageRenderRequest(requestId: Int, source: String) {
        if (!pendingPageRenderRequest.compareAndSet(requestId, null)) {
            return
        }
        Logger.d(this, "[HTML] Web page render completed with source=$source")
        onPageLoadedCallback?.invoke(source)
    }

    internal fun createLocalResourceResponse(url: String): WebResourceResponse? {
        val localResource = LocalResourceUrlMapper.parse(url) ?: return null
        val component = Exponea.getComponent()
        if (component == null) {
            Logger.e(this, "[HTML] Local resource cannot be served, SDK is not initialized")
            return null
        }
        val resourceFile = when (localResource.type) {
            LocalResourceUrlMapper.ResourceType.IMAGE -> component.drawableCache.getFile(localResource.originalUrl)
            LocalResourceUrlMapper.ResourceType.FONT -> component.fontCache.getFontFile(localResource.originalUrl)
        }
        if (resourceFile == null || !resourceFile.exists()) {
            Logger.e(this, "[HTML] Local resource is missing from cache: ${localResource.originalUrl}")
            return null
        }
        Logger.d(
            this,
            "[HTML] Serving local ${localResource.type.name.lowercase()} resource from cache: " +
                localResource.originalUrl
        )
        return runCatching {
            WebResourceResponse(
                detectMimeType(resourceFile, localResource),
                null,
                FileInputStream(resourceFile)
            )
        }.getOrElse {
            Logger.e(this, "[HTML] Local resource cannot be opened: ${localResource.originalUrl}", it)
            null
        }
    }

    internal fun createResourceResponse(url: String): WebResourceResponse? {
        createLocalResourceResponse(url)?.let {
            return it
        }
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            Logger.w(this, "[HTML] Blocking external resource request from WebView: $url")
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }
        return null
    }

    private fun detectMimeType(
        resourceFile: File,
        localResource: LocalResourceUrlMapper.LocalResource
    ): String {
        return URLConnection.guessContentTypeFromName(localResource.originalUrl)
            ?: URLConnection.guessContentTypeFromName(resourceFile.name)
            ?: when (localResource.type) {
                LocalResourceUrlMapper.ResourceType.IMAGE -> "image/png"
                LocalResourceUrlMapper.ResourceType.FONT -> "application/font"
            }
    }

    fun clearContent() {
        skipNextPageLoad = true
        loadedHtmlCrc.set(0)
        loadUrl("about:blank")
    }
}
