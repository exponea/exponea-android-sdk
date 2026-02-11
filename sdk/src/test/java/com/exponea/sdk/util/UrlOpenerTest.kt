package com.exponea.sdk.util

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UrlOpenerTest {

    // ========== NULL & BLANK TESTS ==========

    @Test
    fun `getValidatedUri should return null for null URL`() {
        val result = UrlOpener.getValidatedUri(null)
        assertNull(result)
    }

    @Test
    fun `getValidatedUri should return null for empty string`() {
        val result = UrlOpener.getValidatedUri("")
        assertNull(result)
    }

    @Test
    fun `getValidatedUri should return null for blank string`() {
        val result = UrlOpener.getValidatedUri("   ")
        assertNull(result)
    }

    @Test
    fun `getValidatedUri should return null for whitespace only`() {
        val result = UrlOpener.getValidatedUri("\n\t  \n")
        assertNull(result)
    }

    // ========== TRIM TESTS ==========

    @Test
    fun `getValidatedUri should trim leading spaces`() {
        val result = UrlOpener.getValidatedUri("  https://example.com")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should trim trailing spaces`() {
        val result = UrlOpener.getValidatedUri("https://example.com  ")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should trim both leading and trailing spaces`() {
        val result = UrlOpener.getValidatedUri("  https://example.com  ")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should trim newlines`() {
        val result = UrlOpener.getValidatedUri("\nhttps://example.com\n")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should trim tabs`() {
        val result = UrlOpener.getValidatedUri("\thttps://example.com\t")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    // ========== VALID URLS ==========

    @Test
    fun `getValidatedUri should accept valid HTTPS URL`() {
        val result = UrlOpener.getValidatedUri("https://example.com")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should accept valid HTTP URL`() {
        val result = UrlOpener.getValidatedUri("http://example.com")
        assertNotNull(result)
        assertEquals("http", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should accept custom scheme deep link`() {
        val result = UrlOpener.getValidatedUri("exponea://action/page")
        assertNotNull(result)
        assertEquals("exponea", result.scheme)
        assertEquals("action", result.host)
        assertEquals("/page", result.path)
    }

    @Test
    fun `getValidatedUri should accept URL with path`() {
        val result = UrlOpener.getValidatedUri("https://example.com/path/to/page")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
        assertEquals("/path/to/page", result.path)
    }

    @Test
    fun `getValidatedUri should accept URL with query parameters`() {
        val result = UrlOpener.getValidatedUri("https://example.com?param=value&foo=bar")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
        assertEquals("param=value&foo=bar", result.query)
    }

    @Test
    fun `getValidatedUri should accept URL with fragment`() {
        val result = UrlOpener.getValidatedUri("https://example.com#section")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
        assertEquals("section", result.fragment)
    }

    @Test
    fun `getValidatedUri should accept URL with port`() {
        val result = UrlOpener.getValidatedUri("https://example.com:8080/path")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
        assertEquals(8080, result.port)
        assertEquals("/path", result.path)
    }

    // ========== INVALID SCHEME TESTS ==========

    @Test
    fun `getValidatedUri should reject URL without scheme`() {
        val result = UrlOpener.getValidatedUri("www.example.com")
        assertNull(result)
    }

    @Test
    fun `getValidatedUri should reject URL with only domain`() {
        val result = UrlOpener.getValidatedUri("example.com")
        assertNull(result)
    }

    @Test
    fun `getValidatedUri should reject URL with protocol-relative scheme`() {
        val result = UrlOpener.getValidatedUri("//example.com")
        assertNull(result)
    }

    @Test
    fun `getValidatedUri should reject URL starting with slash`() {
        val result = UrlOpener.getValidatedUri("/path/to/page")
        assertNull(result)
    }

    // ========== REAL-WORLD URLs ==========

    @Test
    fun `getValidatedUri should accept URL with encoded characters`() {
        val result = UrlOpener.getValidatedUri("https://example.com/path?param=hello%20world")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
        assertEquals("param=hello world", result.query)
    }

    @Test
    fun `getValidatedUri should accept URL with special characters in path`() {
        val result = UrlOpener.getValidatedUri(
            "https://example.com/path<test>/page"
        )
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should accept complex deep link`() {
        val result = UrlOpener.getValidatedUri(
            "myapp://product/123?source=email&campaign=summer"
        )
        assertNotNull(result)
        assertEquals("myapp", result.scheme)
        assertEquals("product", result.host)
        assertEquals("/123", result.path)
        assertEquals("source=email&campaign=summer", result.query)
    }

    @Test
    fun `getValidatedUri should accept very long URL`() {
        val longPath = "/very/long/path/" + "segment/".repeat(50)
        val result = UrlOpener.getValidatedUri("https://example.com$longPath")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `getValidatedUri should accept URL with multiple query parameters`() {
        val result = UrlOpener.getValidatedUri(
            "https://example.com?a=1&b=2&c=3&d=4&e=5"
        )
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("a=1&b=2&c=3&d=4&e=5", result.query)
    }

    @Test
    fun `getValidatedUri should accept URL with empty query value`() {
        val result = UrlOpener.getValidatedUri("https://example.com?param=")
        assertNotNull(result)
        assertEquals("param=", result.query)
    }

    @Test
    fun `getValidatedUri should accept URL with username and password`() {
        val result = UrlOpener.getValidatedUri("https://user:pass@example.com/path")
        assertNotNull(result)
        assertEquals("https", result.scheme)
        assertEquals("example.com", result.host)
        assertEquals("user:pass", result.userInfo)
    }
}
