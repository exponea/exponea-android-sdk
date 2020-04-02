package com.exponea.sdk.network

import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.Logger
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor

internal class NetworkHandlerImpl(private var exponeaConfiguration: ExponeaConfiguration) : NetworkHandler {

    private val mediaTypeJson: MediaType = MediaType.parse("application/json")!!
    private lateinit var networkClient: OkHttpClient

    init {
        setupNetworkClient()
    }

    private fun getNetworkInterceptor(): Interceptor {
        return Interceptor {
            var request = it.request()

            Logger.d(this, "Server address: ${request.url().host()}")

            return@Interceptor try {
                it.proceed(request)
            } catch (e: Exception) {
                // Sometimes the request can fail due to SSL problems crashing the app. When that
                // happens, we return a dummy failed request
                Logger.w(this, e.toString())
                val message = "Error: request canceled by $e"
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

        interceptor.level = when (exponeaConfiguration.httpLoggingLevel) {
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

    private fun request(method: String, url: String, authorization: String?, body: String?): Call {
        val requestBuilder = Request.Builder().url(url)

        requestBuilder.addHeader("Content-Type", Constants.Repository.contentType)
        if (authorization != null) {
            requestBuilder.addHeader("Authorization", authorization)
        }

        if (body != null) {
            when (method) {
                "GET" -> requestBuilder.get()
                "POST" -> requestBuilder.post(RequestBody.create(mediaTypeJson, body))
                else -> throw RuntimeException("Http method $method not supported.")
            }
            requestBuilder.post(RequestBody.create(mediaTypeJson, body))
        }

        return networkClient.newCall(requestBuilder.build())
    }

    override fun post(url: String, authorization: String?, body: String?): Call {
        return request("POST", url, authorization, body)
    }

    override fun get(url: String, authorization: String?): Call {
        return request("GET", url, authorization, null)
    }
}
