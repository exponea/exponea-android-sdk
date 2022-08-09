package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.HtmlNormalizer
import io.mockk.mockk
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HtmlNormalizerTests {

    lateinit var context: Context
    lateinit var server: MockWebServer
    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = ExponeaMockServer.createServer()
        File(context.cacheDir, InAppMessageBitmapCacheImpl.DIRECTORY).deleteRecursively()
    }

    @Test
    fun test_closeButtonSingleAction() {
        val rawHtml = "<html><body>" +
                "<div data-actiontype='close'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertNotNull(result.closeActionUrl)
        assertEquals(1, result.actions?.size)
    }

    @Test
    fun test_closeButtonMultipleAction() {
        val rawHtml = "<html><body>" +
                "<div data-actiontype='close'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertNotNull(result.closeActionUrl)
        assertEquals(2, result.actions?.size)
    }

    @Test
    fun test_closeButtonNoAction() {
        val rawHtml = "<html><body>" +
            "<div data-actiontype='close'>Close</div>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertNotNull(result.closeActionUrl)
        assertEquals(0, result.actions?.size)
    }

    @Test
    fun test_NoCloseButtonSingleAction() {
        val rawHtml = "<html><body>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertNotNull(result.closeActionUrl) // default close
        assertEquals(1, result.actions?.size)
    }

    @Test
    fun test_NoCloseButtonMultipleAction() {
        val rawHtml = "<html><body>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertNotNull(result.closeActionUrl) // default close
        assertEquals(2, result.actions?.size)
    }

    @Test
    fun test_NoCloseButtonNoAction() {
        val rawHtml = "<html><body>" +
                "<div>Hello world</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertNotNull(result.closeActionUrl) // default close
        assertEquals(0, result.actions?.size)
    }

    @Test
    fun test_RemoveJavascript() {
        val rawHtml = "<html><body>" +
                "<div data-actiontype='close'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "<script>alert('hello')</script>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertFalse(result.html!!.contains("script"))
    }

    @Test
    fun test_RemoveLink() {
        val rawHtml = "<html>" +
                "<head>" +
                "<link rel='stylesheet' href='styles.css'>" +
                "</head>" +
                "<body>" +
                "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertFalse(result.html!!.contains("<link"))
        assertFalse(result.html!!.contains("styles.css"))
    }

    /**
     * Represents test, that any of HTML JS events are remove
     * https://www.w3schools.com/tags/ref_eventattributes.asp
     */
    @Test
    fun test_RemoveInlineJavascript() {
        val rawHtml = "<html><body>" +
                "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertFalse(result.html!!.contains("onclick"))
    }

    @Test
    fun test_RemoveTitle() {
        val rawHtml = "<html>" +
                "<head><title>Should be removed</title></head>" +
                "<body>" +
                "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertFalse(result.html!!.contains("title"))
        assertFalse(result.html!!.contains("Should be removed"))
    }

    @Test
    fun test_RemoveMeta() {
        val rawHtml = "<html>" +
                "<head>" +
                "<meta name='keywords' content='HTML, CSS, JavaScript'>" +
                "<meta name='author' content='John Doe'>" +
                "</head>" +
                "<body>" +
                "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertFalse(result.html!!.contains("meta"))
        assertFalse(result.html!!.contains("HTML, CSS, JavaScript"))
        assertFalse(result.html!!.contains("John Doe"))
    }

    /**
     * Removes any 'href' from file. Final html contains <a href> but only for close and action buttons and only as final HTML.
     * See possible tags: https://www.w3schools.com/tags/att_href.asp
     */
    @Test
    fun test_RemoveAnyHref() {
        val rawHtml = "<html>" +
            "<head>" +
            "<base href=\"https://example/hreftoremove\" target=\"_blank\">" +
            "<meta name='keywords' content='HTML, CSS, JavaScript'>" +
            "<meta name='author' content='John Doe'>" +
            "<link rel='stylesheet' href='https://example/hreftoremove'>" +
            "</head>" +
            "<body>" +
            "<div href='https://example/hreftoremove'>Unexpected href location</div>" +
            "<map name=\"workmap\">\n" +
            "<area shape=\"rect\" coords=\"34,44,270,350\" alt=\"Computer\" href=\"https://example/hreftoremove\">\n" +
            "</map>" +
            "<a href='https://example/hreftoremove'>Valid anchor link but href has to be removed</a>" +
            "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
            "<div data-link='https://example.com/1'>Action 1</div>" +
            "<div data-link='https://example.com/2'>Action 2</div>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), rawHtml).normalize()
        assertFalse(result.html!!.contains("https://example/hreftoremove"))
        // final HTML has to contain anchor links, but only for close and action buttons
        assertNotNull(result.closeActionUrl)
        assertEquals(2, result.actions?.size)
    }

    @Test
    fun test_OfflineImage() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val rawHtml = "<html>" +
                "<body>" +
                "<img src='$gullImageUrl'>" +
                "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        val result = HtmlNormalizer(bitmapCache, rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("upload.wikimedia.org"))
        assertTrue(result.html!!.contains("data:image/png;base64"))
    }

    @Test
    fun test_InvalidImage() {
        val rawHtml = "<html>" +
                "<body>" +
                "<img src='https://nonexisting.sk/image_that_not_exists.jpg'>" +
                "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        var bitmapCache = InAppMessageBitmapCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, rawHtml).normalize()
        assertFalse(result.valid)
    }
}
