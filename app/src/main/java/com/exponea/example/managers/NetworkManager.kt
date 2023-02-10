package com.exponea.example.managers

import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody

class NetworkManager {
    private val mediaTypeJson: MediaType = "application/json".toMediaTypeOrNull()!!
    private lateinit var networkClient: OkHttpClient

    init {
        setupNetworkClient()
    }

    private fun getNetworkInterceptor(): Interceptor {
        return Interceptor {
            val request = it.request()
            return@Interceptor try {
                it.proceed(request)
            } catch (e: Exception) {
                // Sometimes the request can fail due to SSL problems crashing the app. When that
                // happens, we return a dummy failed request
                val message = "Error: request canceled by $e"
                Response.Builder()
                    .code(400)
                    .protocol(Protocol.HTTP_2)
                    .message(message)
                    .request(it.request())
                    .body(ResponseBody.create("text/plain".toMediaTypeOrNull(), message))
                    .build()
            }
        }
    }

    private fun setupNetworkClient() {
        val networkInterceptor = getNetworkInterceptor()

        networkClient = OkHttpClient.Builder()
            .addInterceptor(networkInterceptor)
            .build()
    }

    private fun request(method: String, url: String, authorization: String?, body: String?): Call {
        val requestBuilder = Request.Builder().url(url)

        requestBuilder.addHeader("Content-Type", "application/json")
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

    fun post(url: String, authorization: String?, body: String?): Call {
        return request("POST", url, authorization, body)
    }

    fun get(url: String, authorization: String?): Call {
        return request("GET", url, authorization, null)
    }
}
