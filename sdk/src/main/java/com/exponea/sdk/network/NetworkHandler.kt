package com.exponea.sdk.network

import com.exponea.sdk.network.auth.AuthStrategy
import okhttp3.Call

internal interface NetworkHandler {
    fun post(url: String, authStrategy: AuthStrategy, body: String): Call
    fun get(url: String, authStrategy: AuthStrategy = AuthStrategy.None): Call
}
