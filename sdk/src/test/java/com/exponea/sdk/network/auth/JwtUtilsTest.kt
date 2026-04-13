package com.exponea.sdk.network.auth

import android.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class JwtUtilsTest {

    private fun buildJwt(payloadJson: String): String {
        val header = Base64.encodeToString(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        val payload = Base64.encodeToString(
            payloadJson.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "$header.$payload.fake-signature"
    }

    @Test
    fun `should parse expiration from valid JWT`() {
        val token = buildJwt("""{"exp":1700000000}""")
        assertEquals(1700000000L, JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should not parse JWT with Bearer prefix`() {
        val token = "Bearer " + buildJwt("""{"exp":1700000000}""")
        assertNull(JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should parse large expiration value`() {
        val token = buildJwt("""{"exp":4102444800}""")
        assertEquals(4102444800L, JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should return null for JWT without exp claim`() {
        val token = buildJwt("""{"sub":"user123"}""")
        assertNull(JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should return null when exp is zero`() {
        val token = buildJwt("""{"exp":0}""")
        assertNull(JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should return null when exp is negative`() {
        val token = buildJwt("""{"exp":-100}""")
        assertNull(JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should return null for empty string`() {
        assertNull(JwtUtils.parseJwtExpiration(""))
    }

    @Test
    fun `should return null for single part token`() {
        assertNull(JwtUtils.parseJwtExpiration("not-a-jwt"))
    }

    @Test
    fun `should return null for two part token`() {
        assertNull(JwtUtils.parseJwtExpiration("header.payload"))
    }

    @Test
    fun `should return null for four part token`() {
        assertNull(JwtUtils.parseJwtExpiration("a.b.c.d"))
    }

    @Test
    fun `should return null for invalid base64 payload`() {
        assertNull(JwtUtils.parseJwtExpiration("header.!!!invalid!!!.signature"))
    }

    @Test
    fun `should return null for non-JSON payload`() {
        val token = buildJwt("this-is-not-json")
        assertNull(JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should handle payload with extra claims`() {
        val token = buildJwt("""{"sub":"user","exp":1700000000,"iat":1699999000}""")
        assertEquals(1700000000L, JwtUtils.parseJwtExpiration(token))
    }

    @Test
    fun `should return null when exp is a string`() {
        val token = buildJwt("""{"exp":"not-a-number"}""")
        assertNull(JwtUtils.parseJwtExpiration(token))
    }
}
