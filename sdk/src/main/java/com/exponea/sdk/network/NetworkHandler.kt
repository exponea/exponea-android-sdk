package com.exponea.sdk.network

import okhttp3.Call

internal interface NetworkHandler {
    fun post(endpoint: String, body: String?): Call
    fun get(endpoint: String, body: String?): Call
}