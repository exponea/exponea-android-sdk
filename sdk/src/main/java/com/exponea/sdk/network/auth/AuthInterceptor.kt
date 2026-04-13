package com.exponea.sdk.network.auth

import com.exponea.sdk.models.ExponeaConfiguration.Companion.BEARER_AUTH_PREFIX
import com.exponea.sdk.models.SdkAuthCallback
import com.exponea.sdk.models.SdkAuthError
import com.exponea.sdk.models.SdkAuthErrorCode
import com.exponea.sdk.repository.AuthTokenRepository
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.services.AuthorizationProvider
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.concurrent.locks.ReentrantLock
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Interceptor that adds authorization header to requests based on the provided [AuthStrategy] and handles 401/403
 * retry logic.
 */
internal class AuthInterceptor(
    private val tokenRepository: AuthTokenRepository,
    private val customerIdsRepository: CustomerIdsRepository,
    private val legacyAuthorizationProvider: AuthorizationProvider?,
    private val authCallbackProvider: () -> SdkAuthCallback?
) : Interceptor {

    companion object {
        private const val JWT_EXPIRY_WINDOW_SECONDS = 60.0
        private const val TOKEN_EXPIRED_KEY = "token_expired"
    }

    private val tokenRefreshLock = ReentrantLock()

    private val expiryNotificationLock = Any()
    private var lastExpiryNotifiedToken: String? = null

    private enum class ExpiryStatus { NOT_EXPIRING, NEAR_EXPIRY, EXPIRED }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val authStrategy = originalRequest.tag(AuthStrategy::class.java) ?: AuthStrategy.None

        val (token, isAuthRequired) = resolveToken(authStrategy)

        if (token.isNullOrEmpty()) {
            if (isAuthRequired) {
                return buildSynthetic401(originalRequest)
            }
            val response = chain.proceed(originalRequest)
            if (authStrategy is AuthStrategy.Token && response.code == 403) {
                return handle403WithoutAuthHeader(chain, originalRequest, response)
            }
            return response
        }

        val authedRequest = originalRequest.newBuilder()
            .header("Authorization", token)
            .build()
        val response = chain.proceed(authedRequest)

        if (authStrategy is AuthStrategy.Token) {
            when (response.code) {
                401 -> return handle401(chain, originalRequest, response, token)
                403 -> return response
            }
        }

        return response
    }

    private data class TokenResolution(val token: String?, val isRequired: Boolean)

    private fun resolveToken(strategy: AuthStrategy): TokenResolution = when (strategy) {
        is AuthStrategy.None -> TokenResolution(token = null, isRequired = false)

        is AuthStrategy.PublicApiKey -> TokenResolution(
            token = strategy.apiToken,
            isRequired = false
        )

        is AuthStrategy.LegacyToken -> resolveLegacyJwtToken(strategy.publicApiKey, strategy.mode)

        is AuthStrategy.Token -> resolveJwtToken(strategy.mode)
    }

    /**
     * Resolves a JWT token from [AuthTokenRepository].
     *
     * When a token is present, proactively checks for near-expiry / expiry via
     * [handleTokenExpiryIfNeeded]. Depending on the result:
     * - [ExpiryStatus.NOT_EXPIRING]: uses the token as-is (no re-read).
     * - [ExpiryStatus.NEAR_EXPIRY]: re-reads the repository because the callback may have
     *   already provided a fresher token; falls back to the original if nothing changed.
     * - [ExpiryStatus.EXPIRED]: the token was cleared; re-reads the repository for a
     *   callback-supplied replacement and falls through to the "no token" path if absent.
     *
     * When no token is available (initially or after expiry) and mode is [AuthMode.REQUIRED],
     * delegates to [requestTokenViaCallback] which serializes the callback under [tokenRefreshLock].
     */
    private fun resolveJwtToken(mode: AuthMode): TokenResolution {
        val token = tokenRepository.getToken()

        if (token != null) {
            when (handleTokenExpiryIfNeeded(token)) {
                ExpiryStatus.NOT_EXPIRING -> {
                    return TokenResolution(token = addBearerPrefix(token), isRequired = mode == AuthMode.REQUIRED)
                }
                ExpiryStatus.NEAR_EXPIRY -> {
                    val currentToken = tokenRepository.getToken() ?: token
                    return TokenResolution(
                        token = addBearerPrefix(currentToken),
                        isRequired = mode == AuthMode.REQUIRED
                    )
                }
                ExpiryStatus.EXPIRED -> {
                    val refreshedToken = tokenRepository.getToken()
                    if (refreshedToken != null) {
                        return TokenResolution(
                            token = addBearerPrefix(refreshedToken),
                            isRequired = mode == AuthMode.REQUIRED
                        )
                    }
                }
            }
        }

        if (mode == AuthMode.OPTIONAL) {
            return TokenResolution(token = null, isRequired = false)
        }

        return requestTokenViaCallback()
    }

    /**
     * Acquires [tokenRefreshLock], checks whether another thread already provided a token,
     * and if not, invokes [SdkAuthCallback.onAuthFailure] and re-reads from [AuthTokenRepository].
     */
    private fun requestTokenViaCallback(): TokenResolution {
        tokenRefreshLock.lock()
        try {
            val existingToken = tokenRepository.getToken()
            if (existingToken != null) {
                Logger.d(this, "Token already provided by another thread, skipping callback")
                return TokenResolution(token = addBearerPrefix(existingToken), isRequired = true)
            }

            Logger.d(this, "JWT token is required but missing, invoking auth callback")
            notifyAuthFailure(SdkAuthErrorCode.TOKEN_NOT_PROVIDED)

            val refreshedToken = tokenRepository.getToken()
            return TokenResolution(
                token = refreshedToken?.let { addBearerPrefix(it) },
                isRequired = true
            )
        } finally {
            tokenRefreshLock.unlock()
        }
    }

    /**
     * Resolves a JWT token for the legacy auth strategy.
     *
     * Tries [AuthorizationProvider] first, if unavailable, falls back to [publicApiKey].
     */
    private fun resolveLegacyJwtToken(publicApiKey: String?, mode: AuthMode): TokenResolution {
        val token = if (legacyAuthorizationProvider != null) {
            legacyAuthorizationProvider.getAuthorizationToken()?.let { ensureBearerPrefix(it) }
        } else {
            publicApiKey
        }

        return TokenResolution(token = token, isRequired = mode == AuthMode.REQUIRED)
    }

    /**
     * Handles a 401 response for [AuthStrategy.Token].
     *
     * Clears the failed token from [AuthTokenRepository], invokes [SdkAuthCallback.onAuthFailure]
     * to give the host app a chance to provide a fresh token, then retries the request once if
     * a token is now available.
     *
     * Clearing before the callback ensures a clean signal: if [AuthTokenRepository.getToken]
     * returns null after the callback, the host app genuinely did not provide a token.
     * This avoids false "unchanged" results when the callback produces a JWT that is
     * byte-for-byte identical to the failed one (e.g. same-second regeneration with
     * second-precision exp claim).
     *
     * Uses [tokenRefreshLock] to serialize concurrent 401 handling so the callback is not invoked
     * redundantly when multiple requests fail at the same time (thundering-herd protection).
     * When a second thread enters while the first is in the callback, it will either find
     * a fresh token (first thread's callback succeeded) and retry directly, or find null
     * (first thread's callback failed) and invoke the callback itself as a second chance.
     */
    private fun handle401(
        chain: Interceptor.Chain,
        originalRequest: Request,
        failedResponse: Response,
        failedToken: String
    ): Response {
        tokenRefreshLock.lock()
        try {
            val currentToken = tokenRepository.getToken()?.let { addBearerPrefix(it) }
            if (currentToken != null && currentToken != failedToken) {
                Logger.d(this, "Token already refreshed by another thread, retrying after 401")
                failedResponse.close()
                return chain.proceed(
                    originalRequest.newBuilder()
                        .header("Authorization", currentToken)
                        .build()
                )
            }

            tokenRepository.clear()

            val errorCode = parseTokenExpiredFlag(failedResponse)
            Logger.d(this, "Received 401 ($errorCode), invoking auth callback for token refresh")
            notifyAuthFailure(errorCode)

            val newToken = tokenRepository.getToken()?.let { addBearerPrefix(it) }

            if (newToken.isNullOrEmpty()) {
                Logger.w(
                    this,
                    "No token provided via SdkAuthCallback after $errorCode, not retrying"
                )
                return failedResponse
            }

            Logger.d(this, "Retrying request with refreshed token after 401")
            failedResponse.close()
            return chain.proceed(
                originalRequest.newBuilder()
                    .header("Authorization", newToken)
                    .build()
            )
        } finally {
            tokenRefreshLock.unlock()
        }
    }

    /**
     * Handles a 403 response for [AuthStrategy.Token] when the original request had no Authorization header.
     *
     * Acquires [tokenRefreshLock], checks whether another thread already provided a token, and if not,
     * invokes [SdkAuthCallback.onAuthFailure] and retries the request once if a token is now available.
     */
    private fun handle403WithoutAuthHeader(
        chain: Interceptor.Chain,
        originalRequest: Request,
        failedResponse: Response
    ): Response {
        tokenRefreshLock.lock()
        try {
            val existingToken = tokenRepository.getToken()?.let { addBearerPrefix(it) }
            if (existingToken != null) {
                Logger.d(this, "Token already provided by another thread, retrying after 403")
                failedResponse.close()
                return chain.proceed(
                    originalRequest.newBuilder()
                        .header("Authorization", existingToken)
                        .build()
                )
            }

            Logger.d(this, "Received 403 on unauthenticated request, invoking auth callback")
            notifyAuthFailure(SdkAuthErrorCode.TOKEN_NOT_PROVIDED)

            val newToken = tokenRepository.getToken()?.let { addBearerPrefix(it) }

            if (newToken.isNullOrEmpty()) {
                Logger.w(
                    this,
                    "No token provided via SdkAuthCallback after ${SdkAuthErrorCode.TOKEN_NOT_PROVIDED}, not retrying"
                )
                return failedResponse
            }

            Logger.d(this, "Retrying request with token after 403")
            failedResponse.close()
            return chain.proceed(
                originalRequest.newBuilder()
                    .header("Authorization", newToken)
                    .build()
            )
        } finally {
            tokenRefreshLock.unlock()
        }
    }

    /**
     * Peeks at the 401 response body looking for `"token_expired": true`.
     * Returns [SdkAuthErrorCode.TOKEN_EXPIRED] when the flag is present,
     * [SdkAuthErrorCode.TOKEN_REJECTED] otherwise.
     */
    private fun parseTokenExpiredFlag(response: Response): SdkAuthErrorCode {
        return try {
            val body = response.peekBody(1024).string()
            val json = Gson().fromJson(body, JsonObject::class.java)
            if (json?.get(TOKEN_EXPIRED_KEY)?.asBoolean == true) {
                SdkAuthErrorCode.TOKEN_EXPIRED
            } else {
                SdkAuthErrorCode.TOKEN_REJECTED
            }
        } catch (e: Exception) {
            Logger.w(this, "Failed to parse 401 response body: ${e.message}")
            SdkAuthErrorCode.TOKEN_REJECTED
        }
    }

    private fun notifyAuthFailure(errorCode: SdkAuthErrorCode) {
        try {
            authCallbackProvider()
                ?.onAuthFailure(SdkAuthError(errorCode, customerIdsRepository.get().externalIds))
                ?: Logger.w(
                    this,
                    "$errorCode requires a new token but no SdkAuthCallback is registered to provide one"
                )
        } catch (e: Throwable) {
            Logger.e(
                this,
                "SdkAuthCallback.onAuthFailure threw an exception for $errorCode, " +
                    "proceeding as if no token was provided",
                e
            )
        }
    }

    private fun addBearerPrefix(token: String): String = "$BEARER_AUTH_PREFIX$token"

    /** Legacy [AuthorizationProvider] may return tokens with or without the Bearer prefix. */
    private fun ensureBearerPrefix(token: String): String {
        return if (token.startsWith(BEARER_AUTH_PREFIX)) token else addBearerPrefix(token)
    }

    /**
     * Checks whether the given JWT is close to expiration or already expired.
     *
     * **Already expired** (`exp <= now`): acquires [tokenRefreshLock], verifies the repository
     * still holds the same stale token (another thread may have already refreshed it), clears
     * it, and invokes the callback. Always acquires the lock for expired tokens so that
     * concurrent threads wait for an in-flight refresh instead of racing.
     *
     * **Near expiry** (`exp <= now + window`): invokes the callback proactively but does
     * *not* clear the token — it may still be accepted by the server.
     * Uses [expiryNotificationLock] / [lastExpiryNotifiedToken] to deduplicate near-expiry
     * notifications for the same token across threads.
     *
     * The callback is always invoked under [tokenRefreshLock] to avoid racing with [handle401]
     * or [handle403WithoutAuthHeader].
     */
    private fun handleTokenExpiryIfNeeded(rawToken: String): ExpiryStatus {
        val expSeconds = JwtUtils.parseJwtExpiration(rawToken) ?: return ExpiryStatus.NOT_EXPIRING
        val nowSeconds = currentTimeSeconds()

        if (expSeconds.toDouble() > nowSeconds + JWT_EXPIRY_WINDOW_SECONDS) {
            return ExpiryStatus.NOT_EXPIRING
        }

        val alreadyExpired = expSeconds.toDouble() <= nowSeconds

        if (alreadyExpired) {
            tokenRefreshLock.lock()
            try {
                val currentRepoToken = tokenRepository.getToken()
                if (currentRepoToken != null && currentRepoToken != rawToken) {
                    Logger.d(this, "Token already refreshed by another thread, skipping expiry clear")
                } else {
                    Logger.d(this, "SDK auth token is already expired, clearing and invoking callback")
                    tokenRepository.clear()
                    notifyAuthFailure(SdkAuthErrorCode.TOKEN_EXPIRED)
                }
            } finally {
                tokenRefreshLock.unlock()
            }
            return ExpiryStatus.EXPIRED
        }

        val shouldNotify = synchronized(expiryNotificationLock) {
            if (lastExpiryNotifiedToken == rawToken) {
                false
            } else {
                lastExpiryNotifiedToken = rawToken
                true
            }
        }
        if (shouldNotify) {
            tokenRefreshLock.lock()
            try {
                Logger.d(this, "SDK auth token is near expiration, invoking callback")
                notifyAuthFailure(SdkAuthErrorCode.TOKEN_ABOUT_TO_EXPIRE)
            } finally {
                tokenRefreshLock.unlock()
            }
        }
        return ExpiryStatus.NEAR_EXPIRY
    }

    private fun buildSynthetic401(request: Request): Response {
        val message = "Required authorization token is missing."
        Logger.e(this, message)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message(message)
            .body(message.toResponseBody("text/plain".toMediaTypeOrNull()))
            .build()
    }
}
