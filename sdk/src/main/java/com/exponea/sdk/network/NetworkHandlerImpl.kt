package com.exponea.sdk.network

import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
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

            request = request.newBuilder()
                    .addHeader("Content-Type", Constants.Repository.contentType)
                    .addHeader("Authorization", "${exponeaConfiguration.authorization}")
                    .build()

            return@Interceptor it.proceed(request)
        }
    }

    private fun getNetworkLogger(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()

        interceptor.level = HttpLoggingInterceptor.Level.BODY

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
                .url(exponeaConfiguration.baseURL+  endpoint)

        if (body != null) {
            requestBuilder.post(RequestBody.create(mediaTypeJson, body))
        }

        return networkClient.newCall(requestBuilder.build())
    }
}