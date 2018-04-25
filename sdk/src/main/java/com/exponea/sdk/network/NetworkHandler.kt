package com.exponea.sdk.network

import okhttp3.Call

interface NetworkHandler {
    fun post(endpoint: String, body: String?): Call
}