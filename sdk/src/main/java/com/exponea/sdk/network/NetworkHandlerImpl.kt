package com.exponea.sdk.network

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.SdkAuthCallback
import com.exponea.sdk.network.auth.AuthInterceptor
import com.exponea.sdk.network.auth.AuthStrategy
import com.exponea.sdk.repository.AuthTokenRepository
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.services.AuthorizationProvider
import com.exponea.sdk.util.Logger
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.HttpLoggingInterceptor

internal class NetworkHandlerImpl(
    private val exponeaConfiguration: ExponeaConfiguration,
    authTokenRepository: AuthTokenRepository,
    customerIdsRepository: CustomerIdsRepository,
    legacyAuthorizationProvider: AuthorizationProvider?,
    authCallbackProvider: () -> SdkAuthCallback?
) : NetworkHandler {

    private val mediaTypeJson: MediaType = "application/json".toMediaType()
    private val networkClient = OkHttpClient.Builder()
        .addInterceptor(
            AuthInterceptor(
                authTokenRepository,
                customerIdsRepository,
                legacyAuthorizationProvider,
                authCallbackProvider
            )
        )
        .addInterceptor(getHttpLoggingInterceptor())
        .addInterceptor(getNetworkInterceptor())
        // keep after logging due to body logging
        .addInterceptor(BrotliInterceptor)
        .build()

    private fun getNetworkInterceptor(): Interceptor {
        return Interceptor {
            val request = it.request()

            Logger.d(this, "Server address: ${request.url.host}")

            return@Interceptor try {
                it.proceed(request)
            } catch (e: Exception) {
                // Sometimes the request can fail due to SSL problems crashing the app. When that
                // happens, we return a dummy failed request
                Logger.w(this, e.toString())
                val message = "Error: request canceled by $e"
                Response.Builder()
                    .code(400)
                    .protocol(Protocol.HTTP_1_1)
                    .message(message)
                    .request(it.request())
                    .body(message.toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
        }
    }

    private fun getHttpLoggingInterceptor() = HttpLoggingInterceptor().apply {
        level = when (exponeaConfiguration.httpLoggingLevel) {
            ExponeaConfiguration.HttpLoggingLevel.NONE -> HttpLoggingInterceptor.Level.NONE
            ExponeaConfiguration.HttpLoggingLevel.BASIC -> HttpLoggingInterceptor.Level.BASIC
            ExponeaConfiguration.HttpLoggingLevel.HEADERS -> HttpLoggingInterceptor.Level.HEADERS
            ExponeaConfiguration.HttpLoggingLevel.BODY -> HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun requestBuilder(
        url: String,
        authStrategy: AuthStrategy,
        headers: Map<String, String> = emptyMap()
    ): Request.Builder = Request.Builder()
        .url(url)
        .addHeader("Content-Type", mediaTypeJson.toString())
        .apply {
            headers.forEach { (name, value) ->
                addHeader(name, value)
            }
        }
        .tag(AuthStrategy::class.java, authStrategy)

    override fun post(
        url: String,
        authStrategy: AuthStrategy,
        body: String,
        headers: Map<String, String>
    ) = networkClient.newCall(
        requestBuilder(url, authStrategy, headers)
            .post(body.toRequestBody(mediaTypeJson))
            .build()
    )

    override fun get(url: String, authStrategy: AuthStrategy) = networkClient.newCall(
        requestBuilder(url, authStrategy)
            .get()
            .build()
    )
}
