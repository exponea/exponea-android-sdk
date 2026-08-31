package com.exponea.sdk.services

import android.content.Context
import android.net.Uri
import android.view.View
import android.webkit.WebResourceRequest
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.AppInboxManagerImplTest
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.LocalResourceUrlMapper
import com.exponea.sdk.view.AppInboxDetailView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class AppInboxProviderTest : ExponeaSDKTest() {

    @Before
    fun before() {
        // Need to be initialized to use bitmapCache for HTML parser
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(
            context,
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(
                    baseUrl = "https://base-url.com",
                    projectToken = "project-token",
                    authorization = "Token auth"
                )
            )
        )
    }

    @Test
    fun `should show empty view for missing message`() {
        mockkObject(Exponea)
        every { Exponea.fetchAppInboxItem(any(), any()) } answers {
            secondArg<(MessageItem?) -> Unit>().invoke(null)
        }
        val provider = DefaultAppInboxProvider()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = provider.getAppInboxDetailView(context, "id1")
        val htmlDetailView = view as AppInboxDetailView
        assertEquals(View.GONE, htmlDetailView.htmlContainer.visibility)
        assertEquals(View.GONE, htmlDetailView.pushContainer.visibility)
    }

    @Test
    fun `should show empty view for unknown type`() {
        mockkObject(Exponea)
        every { Exponea.fetchAppInboxItem(any(), any()) } answers {
            secondArg<(MessageItem?) -> Unit>().invoke(AppInboxManagerImplTest.buildMessage(
                id = "id1",
                type = "blablabla",
                data = mapOf(
                    "title" to "Title",
                    "pre_header" to "Message",
                    "message" to AppInboxManagerImplTest.buildHtmlMessageContent()
                )))
        }
        val provider = DefaultAppInboxProvider()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = provider.getAppInboxDetailView(context, "id1")
        val htmlDetailView = view as AppInboxDetailView
        assertEquals(View.GONE, htmlDetailView.htmlContainer.visibility)
        assertEquals(View.GONE, htmlDetailView.pushContainer.visibility)
    }

    @Test
    fun `should show push notification view`() {
        mockkObject(Exponea)
        every { Exponea.fetchAppInboxItem(any(), any()) } answers {
            secondArg<(MessageItem?) -> Unit>().invoke(AppInboxManagerImplTest.buildMessage(
                id = "id1", type = "html", data = mapOf(
                "title" to "Title",
                "pre_header" to "Message",
                "message" to AppInboxManagerImplTest.buildHtmlMessageContent()
            )))
        }
        val provider = DefaultAppInboxProvider()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = provider.getAppInboxDetailView(context, "id1")
        val htmlDetailView = view as AppInboxDetailView
        assertEquals(View.VISIBLE, htmlDetailView.htmlContainer.visibility)
        assertEquals(View.GONE, htmlDetailView.pushContainer.visibility)
    }

    @Test
    fun `should show html notification view`() {
        mockkObject(Exponea)
        every { Exponea.fetchAppInboxItem(any(), any()) } answers {
            secondArg<(MessageItem?) -> Unit>().invoke(AppInboxManagerImplTest.buildMessage(
                id = "id1", type = "html", data = mapOf(
                "title" to "Title",
                "pre_header" to "Message",
                "message" to AppInboxManagerImplTest.buildHtmlMessageContent()
            )))
        }
        val provider = DefaultAppInboxProvider()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = provider.getAppInboxDetailView(context, "id1")
        val htmlDetailView = view as AppInboxDetailView
        assertEquals(View.VISIBLE, htmlDetailView.htmlContainer.visibility)
        assertEquals(View.GONE, htmlDetailView.pushContainer.visibility)
    }

    @Test
    fun `should serve cached local resources from html detail webview client`() {
        val originalImageUrl = "https://example.com/image.png"
        val cachedFile = Exponea.getComponent()!!.drawableCache.fileCache.retrieveFileDirectly(originalImageUrl)
        cachedFile.parentFile?.mkdirs()
        cachedFile.writeText("cached-image")
        mockkObject(Exponea)
        every { Exponea.fetchAppInboxItem(any(), any()) } answers {
            secondArg<(MessageItem?) -> Unit>().invoke(AppInboxManagerImplTest.buildMessage(
                id = "id1", type = "html", data = mapOf(
                    "message" to "<img src=\"$originalImageUrl\">"
                )
            ))
        }
        val provider = DefaultAppInboxProvider()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detailView = provider.getAppInboxDetailView(context, "id1") as AppInboxDetailView
        val client = shadowOf(detailView.webView).webViewClient
        val localUrl = LocalResourceUrlMapper.imageUrl(originalImageUrl)
        val request = mockk<WebResourceRequest> {
            every { url } returns Uri.parse(localUrl)
        }

        val response = client.shouldInterceptRequest(detailView.webView, request)

        assertNotNull(response)
        assertEquals("image/png", response!!.mimeType)
        assertEquals("cached-image", response.data.bufferedReader().readText())
        cachedFile.delete()
    }

    @Test
    fun `should block non local HTTPS resources from html detail webview client`() {
        mockkObject(Exponea)
        every { Exponea.fetchAppInboxItem(any(), any()) } answers {
            secondArg<(MessageItem?) -> Unit>().invoke(AppInboxManagerImplTest.buildMessage(
                id = "id1", type = "html", data = mapOf("message" to "<p>Content</p>")
            ))
        }
        val provider = DefaultAppInboxProvider()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detailView = provider.getAppInboxDetailView(context, "id1") as AppInboxDetailView
        val client = shadowOf(detailView.webView).webViewClient
        val request = mockk<WebResourceRequest> {
            every { url } returns Uri.parse("https://example.com/image.png")
        }

        val response = client.shouldInterceptRequest(detailView.webView, request)

        assertNotNull(response)
        assertEquals("text/plain", response!!.mimeType)
        assertEquals("", response.data.bufferedReader().readText())
    }
}
