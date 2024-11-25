package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.HtmlActionType
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.HtmlNormalizer.HtmlNormalizerConfig
import io.mockk.mockk
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
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
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(2, result.actions.size)
    }

    @Test
    fun test_closeButtonMultipleAction() {
        val rawHtml = "<html><body>" +
                "<div data-actiontype='close'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(3, result.actions.size)
    }

    @Test
    fun test_datalinkAnchorAction_SameAction() {
        val rawHtml = "<html><body>" +
            "<div data-link='https://example.com/1'>Action 1</div>" +
            "<a href='https://example.com/1'>Action 2</a>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize(config = HtmlNormalizerConfig(
            makeResourcesOffline = false, ensureCloseButton = false
        ))
        assertNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(1, result.actions.size)
    }

    @Test
    fun test_datalinkAnchorAction_MultipleActions() {
        val rawHtml = "<html><body>" +
            "<div data-link='https://example.com/1'>Action 1</div>" +
            "<a href='https://example.com/2'>Action 2</a>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize(config = HtmlNormalizerConfig(
            makeResourcesOffline = false, ensureCloseButton = false
        ))
        assertNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(2, result.actions.size)
    }

    @Test
    fun test_anchorActionWithTargetRemoved() {
        val rawHtml = "<html><body>" +
            "<a href='https://example.com/1' target='_self'>Action 2</a>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize(config = HtmlNormalizerConfig(
            makeResourcesOffline = false, ensureCloseButton = false
        ))
        assertNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(1, result.actions.size)
        assertFalse { result.html!!.contains("target") }
    }

    @Test
    fun test_closeButtonNoAction() {
        val rawHtml = "<html><body>" +
            "<div data-actiontype='close'>Close</div>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(1, result.actions.size)
    }

    @Test
    fun test_NoCloseButtonSingleAction() {
        val rawHtml = "<html><body>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE }) // default close
        assertEquals(2, result.actions.size)
    }

    @Test
    fun test_NoCloseButtonMultipleAction() {
        val rawHtml = "<html><body>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE }) // default close
        assertEquals(3, result.actions.size)
    }

    @Test
    fun test_NoCloseButtonNoAction() {
        val rawHtml = "<html><body>" +
                "<div>Hello world</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE }) // default close
        assertEquals(1, result.actions.size)
    }

    @Test
    fun test_RemoveJavascript() {
        val rawHtml = "<html><body>" +
                "<div data-actiontype='close'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "<script>alert('hello')</script>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
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
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
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
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
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
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
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
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertFalse(result.html!!.contains("meta"))
        assertFalse(result.html!!.contains("HTML, CSS, JavaScript"))
        assertFalse(result.html!!.contains("John Doe"))
    }

    @Test
    fun test_RemoveMeta_exceptViewport() {
        val rawHtml = "<html>" +
                "<head>" +
                "<meta name='keywords' content='HTML, CSS, JavaScript'>" +
                "<meta name='author' content='John Doe'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1' />" +
                "</head>" +
                "<body>" +
                "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
                "<div data-link='https://example.com/1'>Action 1</div>" +
                "<div data-link='https://example.com/2'>Action 2</div>" +
                "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertTrue(result.html!!.contains("meta"))
        assertTrue(result.html!!.contains("viewport"))
        assertFalse(result.html!!.contains("keywords"))
        assertFalse(result.html!!.contains("author"))
        assertFalse(result.html!!.contains("HTML, CSS, JavaScript"))
        assertFalse(result.html!!.contains("John Doe"))
    }

    @Test
    fun test_KeepNormalHref() {
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
            "<a href='https://example.com/anchor'>Valid anchor link</a>" +
            "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
            "<div data-link='https://example.com/1'>Action 1</div>" +
            "<div data-link='https://example.com/2'>Action 2</div>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(4, result.actions.size)
    }

    @Test
    fun test_KeepDataLinkHref() {
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
            "<a href='https://example.com/anchor' data-link='https://example.com/anchor'>Valid anchor link</a>" +
            "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
            "<div data-link='https://example.com/1'>Action 1</div>" +
            "<div data-link='https://example.com/2'>Action 2</div>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(4, result.actions.size)
    }

    @Test
    fun test_KeepDataLinkHref_DataLinkIsPrior() {
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
            "<a href='https://example.com/anchor' data-link='https://example.com/anchor2'>Valid anchor link</a>" +
            "<div data-actiontype='close' onclick='alert('hello')'>Close</div>" +
            "<div data-link='https://example.com/1'>Action 1</div>" +
            "<div data-link='https://example.com/2'>Action 2</div>" +
            "</body></html>"
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(4, result.actions.size)
        assertTrue { result.actions.any { it.actionUrl == "https://example.com/anchor2" } }
        assertTrue { result.actions.none { it.actionUrl == "https://example.com/anchor" } }
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
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("Gull_portrait_ca_usa"))
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
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertFalse(result.valid)
    }

    @Test
    fun test_ActionNameIsUserReadable() {
        val rawHtml = "<html><body>" +
            "<div data-actiontype='close'><span>Close</span></div>" +
            "<div data-link='https://example.com/1'><span>Action 1</span></div>" +
            "<div data-link='https://example.com/2'><span><h1>Action</h1></span><span> 2</span></div>" +
            "</body></html>"
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertNotNull(result.actions.find { it.actionType == HtmlActionType.CLOSE })
        assertEquals(3, result.actions.size)
        val action1 = result.actions.firstOrNull { it.actionUrl == "https://example.com/1" }
        assertNotNull(action1)
        val action2 = result.actions.firstOrNull { it.actionUrl == "https://example.com/2" }
        assertNotNull(action2)
        assertEquals("Action 1", action1.buttonText)
        assertEquals("Action 2", action2.buttonText)
    }

    @Test
    fun test_OfflineImageInCss() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val rawHtml =
            """
            <html>
                <head>
                    <style>
                        .bg-image {
                            background-image: url('$gullImageUrl')
                        }
                    </style>
                </head>
                <body>
                    <div class="bg-image"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("Gull_portrait_ca_usa"))
        assertTrue(result.html!!.contains("data:image/png;base64"))
    }

    @Test
    fun test_OfflineImageInStyleAttr_apostrophes() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val rawHtml =
            """
            <html>
                <body>
                    <div style="background-image: url('$gullImageUrl')"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("Gull_portrait_ca_usa"))
        assertTrue(result.html!!.contains("data:image/png;base64"))
    }

    @Test
    fun test_OfflineImageInStyleAttr_quotes() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val rawHtml =
            """
            <html>
                <body>
                    <div style='background-image: url("$gullImageUrl")'></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("Gull_portrait_ca_usa"))
        assertTrue(result.html!!.contains("data:image/png;base64"))
    }

    @Test
    fun test_OfflineImageInStyleAttr_non_quotes() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val rawHtml =
            """
            <html>
                <body>
                    <div style="background-image: url($gullImageUrl)"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("Gull_portrait_ca_usa"))
        assertTrue(result.html!!.contains("data:image/png;base64"))
    }

    @Test
    fun test_OfflineImageInStyleAttr_regexpStressTest() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val variations = listOf(
            "url($gullImageUrl)",
            "url($gullImageUrl)  ",
            "   url($gullImageUrl)  ",
            "url('$gullImageUrl')",
            "url('$gullImageUrl')  ",
            "   url('$gullImageUrl')  ",
            "url(\"$gullImageUrl\")",
            "url(\"$gullImageUrl\")  ",
            "   url(\"$gullImageUrl\")  "
        )
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        for (each in variations) {
            val quote = if (each.contains("'")) {
                "\""
            } else {
                "'"
            }
            val rawHtml =
                """
            <html>
                <body>
                    <div style=${quote}background-image:$each$quote></div>
                </body>
            </html>
            """.trimIndent()
            val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
            assertTrue(result.valid, "Invalid for usage '$each'")
            assertFalse(result.html!!.contains("Gull_portrait_ca_usa"), "Invalid for usage '$each'")
            assertTrue(result.html!!.contains("data:image/png;base64"), "Invalid for usage '$each'")
        }
    }

    @Test
    fun test_OfflineImageInStyleAttr_Relative() {
        val rawHtml =
            """
            <html>
                <body>
                    <div style="background-image: url('/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg')"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertFalse { result.valid }
    }

    @Test
    fun test_OfflineImageInStyleAttr_Invalid() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val rawHtml =
            """
            <html>
                <body>
                    <div style="background-image: '$gullImageUrl'"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertTrue { result.valid }
        // invalid links should stay intact
        assertTrue(result.html!!.contains("Gull_portrait_ca_usa"))
    }

    @Test
    fun test_OfflineImageInStyleAttr_InvalidName() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val rawHtml =
            """
            <html>
                <body>
                    <div style="background-img: url('$gullImageUrl')"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, mockk(), rawHtml).normalize()
        assertTrue { result.valid }
        // invalid links should stay intact
        assertTrue(result.html!!.contains("Gull_portrait_ca_usa"))
    }

    @Test
    fun test_CssAttributesSelfTest() {
        // tests if HtmlNormalizer impl is correct
        HtmlNormalizer.INLINE_SCRIPT_ATTRIBUTES.forEach {
            assertEquals(it.lowercase(), it)
        }
        HtmlNormalizer.SUPPORTED_CSS_URL_PROPERTIES.forEach {
            assertEquals(it.lowercase(), it)
        }
    }

    @Test
    fun test_OfflineImageInStyleAttr_allPossibilities() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        // list from https://developer.mozilla.org/en-US/docs/Web/CSS/url
        val usageOptions = listOf(
            Pair("background", "url('$gullImageUrl') bottom right repeat-x blue"),
            Pair("background-image", "url('$gullImageUrl')"),
            Pair("background-image", "cross-fade(20% url('$gullImageUrl'), url('$gullImageUrl'))"),
            Pair("border-image", "url('$gullImageUrl') 30 fill / 30px / 30px space"),
            Pair("border-image-source", "url('$gullImageUrl')"),
            Pair("content", "url('$gullImageUrl')"),
            Pair("content", "url('$gullImageUrl') url('$gullImageUrl') url('$gullImageUrl')"),
            Pair("cursor", "url('$gullImageUrl')"),
            Pair("cursor", "url('$gullImageUrl'), pointer"),
            Pair("filter", "url('$gullImageUrl')"),
            Pair("list-style", "url('$gullImageUrl')"),
            Pair("list-style", "lower-roman url('$gullImageUrl') outside"),
            Pair("list-style-image", "url('$gullImageUrl')"),
            Pair("mask", "url('$gullImageUrl')"),
            Pair("mask", "url('$gullImageUrl') 40px 20px"),
            Pair("mask-image", "url('$gullImageUrl')"),
            Pair("mask-image",
                "image(url('$gullImageUrl'), skyblue, linear-gradient(rgba(0, 0, 0, 1.0), transparent))"),
            Pair("offset-path", "url('$gullImageUrl')"),
            Pair("src", "url('$gullImageUrl')"),
            Pair("src", "url('$gullImageUrl') format('woff'), url('$gullImageUrl') format('opentype')")
        )
        // self-test
        for (each in usageOptions) {
            assertTrue(
                HtmlNormalizer.SUPPORTED_CSS_URL_PROPERTIES.contains(each.first),
                "Css property ${each.first} is not listed in HtmlNormalizer css props"
            )
        }
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val fontCache = FontCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        for (each in usageOptions) {
            val rawHtml =
                """
            <html>
                <body>
                    <div style="${each.first}: ${each.second}"></div>
                </body>
            </html>
            """.trimIndent()
            val result = HtmlNormalizer(bitmapCache, fontCache, rawHtml).normalize()
            assertTrue(result.valid, "Invalid for usage '${each.first}: ${each.second}'")
            assertFalse(
                result.html!!.contains("Gull_portrait_ca_usa"),
                "Invalid for usage '${each.first}: ${each.second}'"
            )
            if (each.first == "src") {
                assertTrue(
                    result.html!!.contains("data:application/font;charset=utf-8;base64"),
                    "Invalid for usage '${each.first}: ${each.second}'"
                )
            } else {
                assertTrue(
                    result.html!!.contains("data:image/png;base64"),
                    "Invalid for usage '${each.first}: ${each.second}'"
                )
            }
        }
    }

    @Test
    fun test_OfflineImageInStyleAttr_allPossibilities_in_single_line() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val gullImageUrl = server.url(
            "https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg"
        ).toString()
        val fontUrl = server.url(
            "https://fonts.googleapis.com/css2?family=Roboto:wght@100&display=swap"
        ).toString()
        // list from https://developer.mozilla.org/en-US/docs/Web/CSS/url
        val usageOptions = listOf(
            Pair("background", "url('$gullImageUrl') bottom right repeat-x blue"),
            Pair("background-image", "url('$gullImageUrl')"),
            Pair("background-image", "cross-fade(20% url('$gullImageUrl'), url('$gullImageUrl'))"),
            Pair("border-image", "url('$gullImageUrl') 30 fill / 30px / 30px space"),
            Pair("border-image-source", "url('$gullImageUrl')"),
            Pair("content", "url('$gullImageUrl')"),
            Pair("content", "url('$gullImageUrl') url('$gullImageUrl') url('$gullImageUrl')"),
            Pair("cursor", "url('$gullImageUrl')"),
            Pair("cursor", "url('$gullImageUrl'), pointer"),
            Pair("filter", "url('$gullImageUrl')"),
            Pair("list-style", "url('$gullImageUrl')"),
            Pair("list-style", "lower-roman url('$gullImageUrl') outside"),
            Pair("list-style-image", "url('$gullImageUrl')"),
            Pair("mask", "url('$gullImageUrl')"),
            Pair("mask", "url('$gullImageUrl') 40px 20px"),
            Pair("mask-image", "url('$gullImageUrl')"),
            Pair("mask-image",
                "image(url('$gullImageUrl'), skyblue, linear-gradient(rgba(0, 0, 0, 1.0), transparent))"),
            Pair("offset-path", "url('$gullImageUrl')"),
            Pair("src", "url('$fontUrl')"),
            Pair("src", "url('$fontUrl') format('woff'), url('$fontUrl') format('opentype')")
        )
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val fontCache = FontCacheImpl(context)
        waitForIt { bitmapCache.preload(listOf(gullImageUrl)) { it() } }
        val singleLined = usageOptions
            .joinToString("; ") {
                each -> "${each.first}: ${each.second}"
            }
        val rawHtml =
            """
            <html>
                <body>
                    <div style="$singleLined"></div>
                </body>
            </html>
            """.trimIndent()
        val result = HtmlNormalizer(bitmapCache, fontCache, rawHtml).normalize()
        assertTrue(result.valid)
        assertFalse(result.html!!.contains("Gull_portrait_ca_usa"))
        assertTrue(result.html!!.contains("data:application/font;charset=utf-8;base64"))
        assertTrue(result.html!!.contains("data:image/png;base64"))
    }

    @Test
    fun test_OfflineFontInCss() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val fontUrl = server.url(
            "https://fonts.googleapis.com/css2?family=Roboto:wght@100&display=swap"
        ).toString()
        val rawHtml =
            """
            <html>
                <head>
                    <style>
                        @font-face {
                          font-family: 'Open Sans';
                          src: url($fontUrl) format('woff');
                          font-weight: 700;
                          font-style: normal;
                        }
                    </style>
                </head>
                <body>
                    <div class="bg-image"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val fontCache = FontCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, fontCache, rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("css2"))
        assertFalse(result.html!!.contains("data:image/png;base64"))
        assertTrue(result.html!!.contains("data:application/font;charset=utf-8;base64,"))
    }

    @Test
    fun test_OfflineFontInCss_single_lined() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val fontUrl = server.url(
            "https://fonts.googleapis.com/css2?family=Roboto:wght@100&display=swap"
        ).toString()
        val rawHtml =
            """
            <html>
                <head>
                    <style>
                        @font-face { font-family: 'Open Sans'; src: url($fontUrl) format('woff'); font-weight: 700; font-style: normal; }
                    </style>
                </head>
                <body>
                    <div class="bg-image"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val fontCache = FontCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, fontCache, rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("css2"))
        assertFalse(result.html!!.contains("data:image/png;base64"))
        assertTrue(result.html!!.contains("data:application/font;charset=utf-8;base64,"))
    }

    @Test
    fun test_OfflineFontInCss_Import() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val fontUrl = server.url(
            "https://fonts.googleapis.com/css2?family=Roboto:wght@100&display=swap"
        ).toString()
        val rawHtml =
            """
            <html>
                <head>
                    <style>
                        @import url('$fontUrl');
                    </style>
                </head>
                <body>
                    <div style="font-family: 'Roboto', sans-serif;"></div>
                </body>
            </html>
            """.trimIndent()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val fontCache = FontCacheImpl(context)
        val result = HtmlNormalizer(bitmapCache, fontCache, rawHtml).normalize()
        assertTrue { result.valid }
        assertFalse(result.html!!.contains("css2"))
        assertFalse(result.html!!.contains("data:image/png;base64"))
        assertTrue(result.html!!.contains("data:application/font;charset=utf-8;base64,"))
    }

    @Test
    fun `should keep original class`() {
        val rawHtml = """
        <html><body>
        <div class='test-class-1'>Hello</div>
        <div data-actiontype='close' class='test-class-2'>Close 1</div>
        <a data-actiontype='close' href='https://example.com/close2' class='test-class-3'>Close 2</a>
        <div data-link='https://example.com/1' class='test-class-4'>Action 1</div>
        <a href='https://example.com/2' class='test-class-5'>Action 2</a>
        <a data-link='https://example.com/3' class='test-class-6'>Action 3</a>
        </body></html>
        """.trimIndent()
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize().html
        assertNotNull(result)
        assertTrue(result.contains("class=\"test-class-1\""))
        assertTrue(result.contains("class=\"test-class-2\""))
        assertTrue(result.contains("class=\"test-class-3\""))
        assertTrue(result.contains("class=\"test-class-4\""))
        assertTrue(result.contains("class=\"test-class-5\""))
        assertTrue(result.contains("class=\"test-class-6\""))
    }

    @Test
    fun `should parse all possible close buttons`() {
        val rawHtml = """
        <html><body>
        <div id="1" data-actiontype='close'>Close 1</div>
        <div id="2" data-actiontype='close' data-link='https://example.com/close1'>Close 2</div>
        <a id="3" data-actiontype='close'>Close 3</a>
        <a id="4" data-actiontype='close' href='https://example.com/close2'>Close 4</a>
        <div id="5" data-link='https://example.com/1'>Action 1</div>
        <a id="6" href='https://example.com/2'>Action 2</a>
        <a id="7" href='https://example.com/close2'>Prioritized Action 3</a>
        <a id="8" data-link='https://example.com/3'>Action 4</a>
        <a id="9" data-link='https://example.com/4' href='https://example.com/invalid'>Action 5</a>
        </body></html>
        """.trimIndent()
        val result = HtmlNormalizer(mockk(), mockk(), rawHtml).normalize()
        assertTrue(result.valid)
        assertEquals(9, result.actions.size)
        assertEquals(4, result.actions.filter { it.actionType == HtmlActionType.CLOSE }.size)
        // HTML validation
        val document = Jsoup.parse(result.html!!)
        validateChildElement(document.selectFirst("#1"), HtmlActionType.CLOSE)
        validateChildElement(document.selectFirst("#2"), HtmlActionType.CLOSE)
        validateAhrefElement(document.selectFirst("#3"), HtmlActionType.CLOSE)
        validateAhrefElement(document.selectFirst("#4"), HtmlActionType.CLOSE)
        validateChildElement(document.selectFirst("#5"), HtmlActionType.BROWSER)
        validateAhrefElement(document.selectFirst("#6"), HtmlActionType.BROWSER)
        validateAhrefElement(document.selectFirst("#7"), HtmlActionType.BROWSER)
        validateAhrefElement(document.selectFirst("#8"), HtmlActionType.BROWSER)
        validateAhrefElement(document.selectFirst("#9"), HtmlActionType.BROWSER)
        // Normalizer validation
        assertTrue(result.isActionUrl("https://example.com/1"))
        assertTrue(result.isActionUrl("https://example.com/2"))
        assertTrue(result.isActionUrl("https://example.com/close2"))
        assertEquals(HtmlActionType.CLOSE, result.actions.first { it.buttonText == "Close 1" }.actionType)
        assertEquals(HtmlActionType.CLOSE, result.actions.first { it.buttonText == "Close 2" }.actionType)
        assertEquals(HtmlActionType.CLOSE, result.actions.first { it.buttonText == "Close 3" }.actionType)
        assertEquals(HtmlActionType.CLOSE, result.actions.first { it.buttonText == "Close 4" }.actionType)
        assertEquals(HtmlActionType.BROWSER, result.actions.first { it.buttonText == "Action 1" }.actionType)
        assertEquals(HtmlActionType.BROWSER, result.actions.first { it.buttonText == "Action 2" }.actionType)
        assertEquals(HtmlActionType.BROWSER, result.actions.first {
            it.buttonText == "Prioritized Action 3"
        }.actionType)
    }

    private fun validateChildElement(elementToCheck: Element?, actionType: HtmlActionType) {
        assertNotNull(elementToCheck)
        assertEquals(elementToCheck.attr("data-actiontype"), actionType.value)
        val parentToCheck1 = elementToCheck.parent()
        validateAhrefElement(parentToCheck1, actionType)
        assertEquals(parentToCheck1?.attr("data-link"), elementToCheck.attr("data-link"))
    }

    private fun validateAhrefElement(elementToCheck: Element?, actionType: HtmlActionType) {
        assertNotNull(elementToCheck)
        assertEquals(true, elementToCheck.`is`("a"))
        assertEquals(actionType.value, elementToCheck.attr("data-actiontype"))
        assertEquals(elementToCheck.attr("data-link"), elementToCheck.attr("href"))
    }
}
