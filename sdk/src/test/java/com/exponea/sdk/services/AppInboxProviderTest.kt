package com.exponea.sdk.services

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.AppInboxManagerImplTest
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.view.AppInboxDetailView
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppInboxProviderTest : ExponeaSDKTest() {

    @Before
    fun before() {
        // Need to be initialized to use bitmapCache for HTML parser
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialProject = ExponeaProject(
            "https://base-url.com",
            "project_token",
            "Token auth"
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(
            baseURL = initialProject.baseUrl,
            projectToken = initialProject.projectToken,
            authorization = initialProject.authorization)
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
}
