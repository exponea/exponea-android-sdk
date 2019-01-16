package com.exponea.sdk.network

import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.Logger
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor

class NetworkHandlerImpl(private var exponeaConfiguration: ExponeaConfiguration) : NetworkHandler {

    private val mediaTypeJson: MediaType = MediaType.parse("application/json")!!
    private lateinit var networkClient: OkHttpClient

    init {
        setupNetworkClient()
    }

    private fun getNetworkInterceptor(): Interceptor {
        return Interceptor {
            var request = it.request()

            Logger.d(this, "Server address: ${exponeaConfiguration.baseURL}")

            request = request.newBuilder()
                    .addHeader("Content-Type", Constants.Repository.contentType)
                    .addHeader("Authorization", "${exponeaConfiguration.authorization}")
                    .build()

            if (exponeaConfiguration.authorization?.contains("Basic") == true) {
                Logger.w(this, "Warning: Basic authentication is deprecated. Use Token authentication instead.")
            }

            return@Interceptor try {
                it.proceed(request)
            } catch (e: Exception) {
                // Sometimes the request can fail due to SSL problems crashing the app. When that
                // happens, we return a dummy failed request
                Logger.w(this, e.toString())
                val message = "Error: request canceled by " + e.toString()
                Response.Builder()
                        .code(400)
                        .protocol(Protocol.HTTP_2)
                        .message(message)
                        .request(it.request())
                        .body(ResponseBody.create(MediaType.parse("text/plain"), message))
                        .build()
            }
        }
    }

    private fun getNetworkLogger(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()

        interceptor.level = when(exponeaConfiguration.httpLoggingLevel) {
            ExponeaConfiguration.HttpLoggingLevel.NONE -> HttpLoggingInterceptor.Level.NONE
            ExponeaConfiguration.HttpLoggingLevel.BASIC -> HttpLoggingInterceptor.Level.BASIC
            ExponeaConfiguration.HttpLoggingLevel.HEADERS -> HttpLoggingInterceptor.Level.HEADERS
            ExponeaConfiguration.HttpLoggingLevel.BODY -> HttpLoggingInterceptor.Level.BODY
        }

        return interceptor
    }

    private fun setupNetworkClient() {
        val networkInterceptor = getNetworkInterceptor()

        networkClient = OkHttpClient.Builder()
                .addInterceptor(getNetworkLogger())
                .addInterceptor(networkInterceptor)
                .build()
    }

    override fun post(endpoint: String, body: String?): Call {
        val requestBuilder = Request.Builder()
                .url(exponeaConfiguration.baseURL + endpoint)

        if (body != null) {
            requestBuilder.post(RequestBody.create(mediaTypeJson, body))
        }

        return networkClient.newCall(requestBuilder.build())
    }

    override fun get(endpoint: String, body: String?): Call {
        val requestBuilder = Request.Builder()
                .url(exponeaConfiguration.baseURL + endpoint)

        if (body != null) {
            requestBuilder.method("GET", RequestBody.create(mediaTypeJson, body))
        }

        return networkClient.newCall(requestBuilder.build())
    }
}