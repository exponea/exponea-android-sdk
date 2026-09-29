package com.exponea.example.managers

import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class NetworkManager {
    private val mediaTypeJson = "application/json".toMediaTypeOrNull()!!
    private val networkClient: OkHttpClient

    init {
        val networkInterceptor = Interceptor { chain ->
            try {
                chain.proceed(chain.request())
            } catch (e: Exception) {
                val message = "Error: request canceled by $e"
                Response.Builder()
                    .code(400)
                    .protocol(Protocol.HTTP_2)
                    .message(message)
                    .request(chain.request())
                    .body(message.toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
        }
        networkClient = OkHttpClient.Builder()
            .addInterceptor(networkInterceptor)
            .build()
    }

    fun post(url: String, authorization: String?, body: String?): Call {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
        if (authorization != null) {
            requestBuilder.addHeader("Authorization", authorization)
        }
        if (body != null) {
            requestBuilder.post(body.toRequestBody(mediaTypeJson))
        }
        return networkClient.newCall(requestBuilder.build())
    }
}
