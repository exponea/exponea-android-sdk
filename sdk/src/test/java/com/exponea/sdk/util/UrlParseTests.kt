package com.exponea.sdk.util

import android.net.Uri
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * We need to be sure that SDK is parsing URLs correctly according to https://www.w3schools.com/tags/ref_urlencode.ASP
 * Android Uri.parse() should work correctly.
 */
@RunWith(RobolectricTestRunner::class)
internal class UrlParseTests {

    @Test
    fun `should parse HTTP Url - simple`() {
        val parsedUrl = Uri.parse("https://www.google.com")
        validateUrl(
            parsedUrl,
            scheme = "https",
            host = "www.google.com"
        )
    }

    @Test
    fun `should parse HTTP Url - path`() {
        val parsedUrl = Uri.parse("https://www.google.com/test")
        validateUrl(
            parsedUrl,
            scheme = "https",
            host = "www.google.com",
            path = "/test"
        )
    }

    @Test
    fun `should parse HTTP Url - path and query`() {
        val parsedUrl = Uri.parse("https://www.google.com/test?name=Hello")
        validateUrl(
            parsedUrl,
            scheme = "https",
            host = "www.google.com",
            path = "/test",
            query = "name=Hello"
        )
    }

    @Test
    fun `should parse Deeplink Url - simple`() {
        val parsedUrl = Uri.parse("myApp://www.google.com")
        validateUrl(
            parsedUrl,
            scheme = "myApp",
            host = "www.google.com"
        )
    }

    @Test
    fun `should parse Deeplink Url - path`() {
        val parsedUrl = Uri.parse("myApp://www.google.com/test")
        validateUrl(
            parsedUrl,
            scheme = "myApp",
            host = "www.google.com",
            path = "/test"
        )
    }

    @Test
    fun `should parse Deeplink Url - path and query`() {
        val parsedUrl = Uri.parse("myApp://www.google.com/test?name=Hello")
        validateUrl(
            parsedUrl,
            scheme = "myApp",
            host = "www.google.com",
            path = "/test",
            query = "name=Hello"
        )
    }

    @Test
    fun `should parse Deeplink Url - prod issue`() {
        val parsedUrl = Uri.parse("""
            pltapp://category/categories<{defaultcategory2_shopby233}/categories<{defaultcategory2_shopby233_backinstock221}?adjust_tracker=74p1fnr&adjust_campaign=PROMOTIONAL&adjust_adgroup=2023-06-19-ALL-FR&adjust_creative=CATEGORY
        """.trimIndent())
        validateUrl(
            parsedUrl,
            scheme = "pltapp",
            host = "category",
            path = "/categories<{defaultcategory2_shopby233}/categories<{defaultcategory2_shopby233_backinstock221}",
            query = """
                adjust_tracker=74p1fnr&adjust_campaign=PROMOTIONAL&adjust_adgroup=2023-06-19-ALL-FR&adjust_creative=CATEGORY
            """.trimIndent()
        )
    }

    @Test
    fun `should parse Deeplink Url - prod issue 2`() {
        val areUrlEquals = URLUtils.areEqualAsURLs(
            """
                pltapp://category/categories<{defaultcategory2_shopby233}/categories<{defaultcategory2_shopby233_backinstock221}?adjust_tracker=74p1fnr&adjust_campaign=PROMOTIONAL&adjust_adgroup=2023-06-19-ALL-FR&adjust_creative=CATEGORY
            """.trimIndent(),
            """
                pltapp://category/categories%3C%7Bdefaultcategory2_shopby233%7D/categories%3C%7Bdefaultcategory2_shopby233_backinstock221%7D?adjust_tracker=74p1fnr&adjust_campaign=PROMOTIONAL&adjust_adgroup=2023-06-19-ALL-FR&adjust_creative=CATEGORY
            """.trimIndent()
        )
        assertTrue(areUrlEquals)
    }

    @Test
    fun `should parse DSN`() {
        val dsnString = "https://abcd69ea7c48f1106a0b8be233443f@12348953780027392.ingest.de.sentry.io/1234953969229904"
        val dnsUri = Uri.parse(dsnString)
        assertEquals("abcd69ea7c48f1106a0b8be233443f", dnsUri.userInfo)
        assertEquals("12348953780027392.ingest.de.sentry.io", dnsUri.host)
        assertEquals("1234953969229904", dnsUri.lastPathSegment)
    }

    private fun validateUrl(
        urlToCheck: Uri,
        scheme: String? = null,
        host: String? = null,
        path: String? = null,
        query: String? = null
    ) {
        assertEquals(scheme, urlToCheck.scheme, "Scheme mismatched")
        assertEquals(host, urlToCheck.host, "Host mismatched")
        if (path == null) {
            assertTrue(urlToCheck.path.isNullOrEmpty())
        } else {
            assertEquals(path, urlToCheck.path, "Path mismatched")
        }
        assertEquals(query, urlToCheck.query, "Query mismatched")
    }
}
