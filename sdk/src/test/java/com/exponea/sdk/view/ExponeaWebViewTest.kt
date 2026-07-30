package com.exponea.sdk.view

import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.LocalResourceUrlMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaWebViewTest : ExponeaSDKTest() {
    @Test
    fun `should create webview with anti-XSS setup`() {
        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())
        // TODO: no getter for geolocation flag
        assertEquals(false, webview.settings.allowFileAccessFromFileURLs)
        assertEquals(false, webview.settings.allowUniversalAccessFromFileURLs)
        assertEquals(false, webview.settings.allowContentAccess)
        assertEquals(false, webview.settings.allowFileAccess)
        assertEquals(false, webview.settings.saveFormData)
        assertEquals(false, webview.settings.savePassword)
        assertEquals(false, webview.settings.javaScriptEnabled)
        assertEquals(false, webview.settings.javaScriptCanOpenWindowsAutomatically)
        assertEquals(false, webview.settings.blockNetworkImage)
        assertEquals(false, webview.settings.blockNetworkLoads)
        assertEquals(false, webview.settings.databaseEnabled)
        assertEquals(false, webview.settings.domStorageEnabled)
        assertEquals(false, webview.settings.loadWithOverviewMode)
        assertEquals(WebSettings.LOAD_NO_CACHE, webview.settings.cacheMode)
    }

    @Test
    fun `should allow cookies if SDK init allows`() {
        initSdk(true)
        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())
        assertEquals(true, CookieManager.getInstance().acceptCookie())
        // TODO: Invalid test! should be TRUE.
        // There is an issue with RoboCookieManager and it returns false always
        // Real usage on device is working expected
        assertEquals(false, CookieManager.getInstance().acceptThirdPartyCookies(webview))
    }

    @Test
    fun `should deny cookies if SDK init denies`() {
        initSdk(false)
        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())
        assertEquals(false, CookieManager.getInstance().acceptCookie())
        assertEquals(false, CookieManager.getInstance().acceptThirdPartyCookies(webview))
    }

    @Test
    fun `should deny cookies by default`() {
        initSdk(null)
        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())
        assertEquals(false, CookieManager.getInstance().acceptCookie())
        assertEquals(false, CookieManager.getInstance().acceptThirdPartyCookies(webview))
    }

    @Test
    fun `should keep system defaults if SDK is not init`() {
        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())
        assertEquals(false, CookieManager.getInstance().acceptCookie())
        assertEquals(false, CookieManager.getInstance().acceptThirdPartyCookies(webview))
    }

    @Test
    fun `should serve local resource url from SDK cache`() {
        initSdk(null)
        val originalUrl = "https://example.com/image.png"
        val cachedFile = Exponea.getComponent()!!.drawableCache.fileCache.retrieveFileDirectly(originalUrl)
        cachedFile.parentFile?.mkdirs()
        cachedFile.writeText("image-data")

        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())
        val response = webview.createLocalResourceResponse(LocalResourceUrlMapper.imageUrl(originalUrl))

        assertNotNull(response)
        assertEquals("image/png", response.mimeType)
        assertEquals("image-data", response.data.bufferedReader().readText())
        cachedFile.delete()
    }

    @Test
    fun `should ignore non-local resource url`() {
        initSdk(null)
        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())

        assertNull(webview.createLocalResourceResponse("https://example.com/image.png"))
    }

    @Test
    fun `should block non-local network resource url`() {
        initSdk(null)
        val webview = ExponeaWebView(ApplicationProvider.getApplicationContext())

        val response = webview.createResourceResponse("https://example.com/image.png")

        assertNotNull(response)
        assertEquals("text/plain", response.mimeType)
        assertEquals("", response.data.bufferedReader().readText())
    }

    private fun initSdk(allowCookies: Boolean?) {
        Exponea.flushMode = FlushMode.MANUAL
        val configuration = ExponeaConfiguration(integrationConfig = ProjectConfig(projectToken = "mock-token"))
        allowCookies?.let {
            configuration.allowWebViewCookies = it
        }
        Exponea.init(
                ApplicationProvider.getApplicationContext(),
                configuration
        )
    }
}
