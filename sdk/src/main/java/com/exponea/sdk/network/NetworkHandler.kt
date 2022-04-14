package com.exponea.sdk.network

import okhttp3.Call

internal interface NetworkHandler {
    fun post(url: String, authorization: String?, body: String?): Call
    fun get(url: String, authorization: String? = null): Call
}
