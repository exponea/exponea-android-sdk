package com.exponea.sdk.network.auth

import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration.Companion.BEARER_AUTH_PREFIX
import com.exponea.sdk.models.SdkAuthCallback
import com.exponea.sdk.models.SdkAuthError
import com.exponea.sdk.models.SdkAuthErrorCode
import com.exponea.sdk.repository.AuthTokenRepository
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.services.AuthorizationProvider
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class AuthInterceptorTest {

    private val tokenRepository: AuthTokenRepository = mockk(relaxed = true)
    private val customerIdsRepository: CustomerIdsRepository = mockk()
    private val mockCallback: SdkAuthCallback = mockk(relaxed = true)
    private val testCustomerIds = CustomerIds(hashMapOf("registered" to "test@example.com"))

    private var authCallback: SdkAuthCallback? = mockCallback

    @Before
    fun setup() {
        mockkObject(Logger)
        every { Logger.d(any(), any()) } just Runs
        every { Logger.w(any(), any()) } just Runs
        every { Logger.e(any(), any()) } just Runs
        every { Logger.e(any(), any(), any()) } just Runs

        every { customerIdsRepository.get() } returns testCustomerIds
        every { tokenRepository.getToken() } returns null
        every { tokenRepository.setToken(any()) } just Runs
        every { tokenRepository.clear() } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createInterceptor(
        legacyAuthProvider: AuthorizationProvider? = null
    ) = AuthInterceptor(
        tokenRepository,
        customerIdsRepository,
        legacyAuthProvider
    ) { authCallback }

    private fun buildRequest(
        authStrategy: AuthStrategy = AuthStrategy.None,
        url: String = "https://api.example.com/test"
    ): Request = Request.Builder()
        .url(url)
        .tag(AuthStrategy::class.java, authStrategy)
        .build()

    private fun buildResponse(
        request: Request,
        code: Int,
        message: String = "OK",
        body: String = "",
        contentType: String = "text/plain"
    ): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(body.toResponseBody(contentType.toMediaTypeOrNull()))
            .build()

    private fun mockChain(
        request: Request,
        vararg responses: Response
    ): Interceptor.Chain {
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        if (responses.size == 1) {
            every { chain.proceed(any()) } returns responses[0]
        } else {
            every { chain.proceed(any()) } returnsMany responses.toList()
        }
        return chain
    }

    private fun mockJwtExpiration(token: String, expSeconds: Long?) {
        mockkObject(JwtUtils)
        every { JwtUtils.parseJwtExpiration(token) } returns expSeconds
    }

    private fun mockCurrentTime(seconds: Double) {
        mockkStatic("com.exponea.sdk.util.ExtensionsKt")
        every { currentTimeSeconds() } returns seconds
    }

    private fun capturedAuthHeader(chain: Interceptor.Chain): String? {
        val requestSlot = slot<Request>()
        verify { chain.proceed(capture(requestSlot)) }
        return requestSlot.captured.header("Authorization")
    }

    @Test
    fun `None strategy proceeds without Authorization header`() {
        val request = buildRequest(AuthStrategy.None)
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), nullValue())
    }

    @Test
    fun `PublicApiKey sets api key as Authorization header`() {
        val request = buildRequest(AuthStrategy.PublicApiKey("my-api-key"))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), equalTo("my-api-key"))
    }

    @Test
    fun `PublicApiKey with null key proceeds without Authorization header`() {
        val request = buildRequest(AuthStrategy.PublicApiKey(null))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), nullValue())
    }

    @Test
    fun `Token strategy adds Bearer prefix to repository token`() {
        val token = "simple-token"
        every { tokenRepository.getToken() } returns token

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), equalTo("${BEARER_AUTH_PREFIX}$token"))
    }

    @Test
    fun `Token OPTIONAL with no token proceeds without Authorization`() {
        every { tokenRepository.getToken() } returns null

        val request = buildRequest(AuthStrategy.Token(AuthMode.OPTIONAL))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), nullValue())
        verify(exactly = 0) { mockCallback.onAuthFailure(any()) }
    }

    @Test
    fun `Token REQUIRED with no token invokes callback and uses provided token`() {
        val newToken = "callback-token"
        // 1st: resolveJwtToken, 2nd: requestTokenViaCallback check, 3rd: after callback
        every { tokenRepository.getToken() } returnsMany listOf(null, null, newToken)

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(errorSlot.captured.errorCode, equalTo(SdkAuthErrorCode.TOKEN_NOT_PROVIDED))
        assertThat(capturedAuthHeader(chain), equalTo("${BEARER_AUTH_PREFIX}$newToken"))
    }

    @Test
    fun `Token REQUIRED with no token and callback provides nothing returns synthetic 401`() {
        every { tokenRepository.getToken() } returns null

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(401))
        assertThat(response.message, equalTo("Required authorization token is missing."))
        verify { mockCallback.onAuthFailure(any()) }
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun `401 triggers TOKEN_REJECTED callback and retries with new token`() {
        val oldToken = "old-token"
        val newToken = "new-token"
        // 1st: resolveJwtToken, 2nd: handle401 check, 3rd: after callback
        every { tokenRepository.getToken() } returnsMany listOf(oldToken, oldToken, newToken)

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 401), buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(errorSlot.captured.errorCode, equalTo(SdkAuthErrorCode.TOKEN_REJECTED))
        verify { tokenRepository.clear() }
    }

    @Test
    fun `401 with token_expired in body triggers TOKEN_EXPIRED callback`() {
        val oldToken = "old-token"
        val newToken = "new-token"
        // 1st: resolveJwtToken, 2nd: handle401 check (same → clear), 3rd: after callback
        every { tokenRepository.getToken() } returnsMany listOf(oldToken, oldToken, newToken)

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val expiredBody = """
            {
                "success":false,
                "error":"token has invalid claims: token is expired",
                "token_expired":true
            }
            """.trimIndent()
        val chain = mockChain(
            request,
            buildResponse(request, 401, body = expiredBody, contentType = "application/json"),
            buildResponse(request, 200)
        )

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(errorSlot.captured.errorCode, equalTo(SdkAuthErrorCode.TOKEN_EXPIRED))
        verify { tokenRepository.clear() }
    }

    @Test
    fun `401 without token_expired in body triggers TOKEN_REJECTED callback`() {
        val oldToken = "old-token"
        val newToken = "new-token"
        // 1st: resolveJwtToken, 2nd: handle401 check (same → clear), 3rd: after callback
        every { tokenRepository.getToken() } returnsMany listOf(oldToken, oldToken, newToken)

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val rejectedBody = """{"success":false,"error":"invalid token"}"""
        val chain = mockChain(
            request,
            buildResponse(request, 401, body = rejectedBody, contentType = "application/json"),
            buildResponse(request, 200)
        )

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(errorSlot.captured.errorCode, equalTo(SdkAuthErrorCode.TOKEN_REJECTED))
    }

    @Test
    fun `401 with no new token after callback returns original 401`() {
        val oldToken = "old-token"
        // 1st: resolveJwtToken, 2nd: handle401 check (same → clear), 3rd: after callback → null
        every { tokenRepository.getToken() } returnsMany listOf(oldToken, oldToken, null)

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 401, "Unauthorized"))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(401))
        verify { mockCallback.onAuthFailure(any()) }
        verify { tokenRepository.clear() }
    }

    @Test
    fun `401 with already-refreshed token retries without invoking callback`() {
        val oldToken = "old-token"
        val refreshedToken = "refreshed-by-other-thread"
        // 1st: resolveJwtToken, 2nd: handle401 check → different token
        every { tokenRepository.getToken() } returnsMany listOf(oldToken, refreshedToken)

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 401), buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        verify(exactly = 0) { mockCallback.onAuthFailure(any()) }
        verify(exactly = 0) { tokenRepository.clear() }
    }

    @Test
    fun `403 on unauthenticated Token OPTIONAL triggers TOKEN_NOT_PROVIDED callback and retries`() {
        val newToken = "new-token"
        // 1st: resolveJwtToken → null, 2nd: handle403 check → null, 3rd: after callback
        every { tokenRepository.getToken() } returnsMany listOf(null, null, newToken)

        val request = buildRequest(AuthStrategy.Token(AuthMode.OPTIONAL))
        val chain = mockChain(request, buildResponse(request, 403), buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(errorSlot.captured.errorCode, equalTo(SdkAuthErrorCode.TOKEN_NOT_PROVIDED))
    }

    @Test
    fun `403 on unauthenticated Token OPTIONAL with no new token returns 403`() {
        every { tokenRepository.getToken() } returns null

        val request = buildRequest(AuthStrategy.Token(AuthMode.OPTIONAL))
        val chain = mockChain(request, buildResponse(request, 403, "Forbidden"))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(403))
        verify { mockCallback.onAuthFailure(any()) }
    }

    @Test
    fun `403 with valid token is returned as-is without callback`() {
        every { tokenRepository.getToken() } returns "valid-token"

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 403, "Forbidden"))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(403))
        verify(exactly = 0) { mockCallback.onAuthFailure(any()) }
    }

    @Test
    fun `non-expiring JWT does not invoke callback`() {
        val token = "valid-jwt"
        val exp = 1772202000L
        every { tokenRepository.getToken() } returns token
        mockJwtExpiration(token, exp)
        mockCurrentTime(exp - 61.0) // 61s before exp

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        verify(exactly = 0) { mockCallback.onAuthFailure(any()) }
    }

    @Test
    fun `near-expiry JWT invokes callback with TOKEN_ABOUT_TO_EXPIRE`() {
        val token = "near-expiry-jwt"
        val exp = 1772202000L
        every { tokenRepository.getToken() } returns token
        mockJwtExpiration(token, exp)
        mockCurrentTime(exp - 59.0) // 59s before exp, within 60s window

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(errorSlot.captured.errorCode, equalTo(SdkAuthErrorCode.TOKEN_ABOUT_TO_EXPIRE))
    }

    @Test
    fun `near-expiry notification is sent only once for the same token`() {
        val token = "near-expiry-jwt"
        val exp = 1772202000L
        every { tokenRepository.getToken() } returns token
        mockJwtExpiration(token, 1772202000)
        mockCurrentTime(exp - 59.0) // 59s before exp, within 60s window

        val interceptor = createInterceptor()

        val request1 = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        interceptor.intercept(mockChain(request1, buildResponse(request1, 200)))

        val request2 = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        interceptor.intercept(mockChain(request2, buildResponse(request2, 200)))

        verify(exactly = 1) { mockCallback.onAuthFailure(any()) }
    }

    @Test
    fun `expired JWT clears repository and invokes callback with TOKEN_EXPIRED`() {
        val expiredToken = "expired-jwt"
        val newToken = "refreshed-token"
        val exp = 1772202000L
        // 1st: resolveJwtToken, 2nd: handleTokenExpiryIfNeeded check, 3rd: resolveJwtToken EXPIRED re-read
        every { tokenRepository.getToken() } returnsMany listOf(expiredToken, expiredToken, newToken)
        mockJwtExpiration(expiredToken, exp)
        mockCurrentTime(exp + 100.0) // 100s after exp

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(errorSlot.captured.errorCode, equalTo(SdkAuthErrorCode.TOKEN_EXPIRED))
        verify { tokenRepository.clear() }
        assertThat(capturedAuthHeader(chain), equalTo("${BEARER_AUTH_PREFIX}$newToken"))
    }

    @Test
    fun `LegacyToken uses AuthorizationProvider token with Bearer prefix`() {
        val legacyProvider = mockk<AuthorizationProvider>()
        every { legacyProvider.getAuthorizationToken() } returns "legacy-jwt"

        val request = buildRequest(AuthStrategy.LegacyToken(publicApiKey = "api-key"))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor(legacyAuthProvider = legacyProvider).intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), equalTo("${BEARER_AUTH_PREFIX}legacy-jwt"))
    }

    @Test
    fun `LegacyToken does not duplicate existing Bearer prefix`() {
        val legacyProvider = mockk<AuthorizationProvider>()
        every { legacyProvider.getAuthorizationToken() } returns "${BEARER_AUTH_PREFIX}already-prefixed"

        val request = buildRequest(AuthStrategy.LegacyToken(publicApiKey = "api-key"))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor(legacyAuthProvider = legacyProvider).intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), equalTo("${BEARER_AUTH_PREFIX}already-prefixed"))
    }

    @Test
    fun `LegacyToken falls back to publicApiKey when provider is null`() {
        val request = buildRequest(AuthStrategy.LegacyToken(publicApiKey = "my-public-key"))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor(legacyAuthProvider = null).intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), equalTo("my-public-key"))
    }

    @Test
    fun `LegacyToken with null provider and null publicApiKey proceeds without Authorization`() {
        val request = buildRequest(AuthStrategy.LegacyToken(publicApiKey = null))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor(legacyAuthProvider = null).intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), nullValue())
    }

    @Test
    fun `no callback set does not crash when token is required`() {
        authCallback = null
        every { tokenRepository.getToken() } returns null

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(401))
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun `no callback set does not crash on 401`() {
        authCallback = null
        val oldToken = "old-token"
        every { tokenRepository.getToken() } returnsMany listOf(oldToken, oldToken, null)

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 401, "Unauthorized"))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(401))
    }

    @Test
    fun `callback receives correct customer external IDs`() {
        every { tokenRepository.getToken() } returns null

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        createInterceptor().intercept(chain)

        val errorSlot = slot<SdkAuthError>()
        verify { mockCallback.onAuthFailure(capture(errorSlot)) }
        assertThat(
            errorSlot.captured.customerIds,
            equalTo(hashMapOf<String, String?>("registered" to "test@example.com") as Map<String, String?>)
        )
    }

    @Test
    fun `callback exception on REQUIRED missing token returns synthetic 401`() {
        every { tokenRepository.getToken() } returns null
        every { mockCallback.onAuthFailure(any()) } throws RuntimeException("host app bug")

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(401))
        assertThat(response.message, equalTo("Required authorization token is missing."))
        verify(exactly = 0) { chain.proceed(any()) }
        verify { Logger.e(any(), any(), any()) }
    }

    @Test
    fun `callback exception on 401 returns original 401 response`() {
        val oldToken = "old-token"
        every { tokenRepository.getToken() } returnsMany listOf(oldToken, oldToken)
        every { mockCallback.onAuthFailure(any()) } throws RuntimeException("host app bug")

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 401, "Unauthorized"))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(401))
        verify { tokenRepository.clear() }
        verify { Logger.e(any(), any(), any()) }
    }

    @Test
    fun `callback exception on 403 unauthenticated returns original 403 response`() {
        every { tokenRepository.getToken() } returns null
        every { mockCallback.onAuthFailure(any()) } throws RuntimeException("host app bug")

        val request = buildRequest(AuthStrategy.Token(AuthMode.OPTIONAL))
        val chain = mockChain(request, buildResponse(request, 403, "Forbidden"))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(403))
        verify { Logger.e(any(), any(), any()) }
    }

    @Test
    fun `callback exception on expired JWT still returns response`() {
        val expiredToken = "expired-jwt"
        val exp = 1772202000L
        every { tokenRepository.getToken() } returnsMany listOf(expiredToken, expiredToken, null)
        mockJwtExpiration(expiredToken, exp)
        mockCurrentTime(exp + 100.0)
        every { mockCallback.onAuthFailure(any()) } throws RuntimeException("host app bug")

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(401))
        verify { tokenRepository.clear() }
        verify { Logger.e(any(), any(), any()) }
    }

    @Test
    fun `callback exception on near-expiry JWT still uses current token`() {
        val token = "near-expiry-jwt"
        val exp = 1772202000L
        every { tokenRepository.getToken() } returns token
        mockJwtExpiration(token, exp)
        mockCurrentTime(exp - 59.0)
        every { mockCallback.onAuthFailure(any()) } throws RuntimeException("host app bug")

        val request = buildRequest(AuthStrategy.Token(AuthMode.REQUIRED))
        val chain = mockChain(request, buildResponse(request, 200))

        val response = createInterceptor().intercept(chain)

        assertThat(response.code, equalTo(200))
        assertThat(capturedAuthHeader(chain), equalTo("${BEARER_AUTH_PREFIX}$token"))
        verify { Logger.e(any(), any(), any()) }
    }
}
