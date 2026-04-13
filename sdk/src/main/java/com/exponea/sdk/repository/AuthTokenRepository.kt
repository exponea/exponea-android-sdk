package com.exponea.sdk.repository

internal interface AuthTokenRepository {
    fun setToken(token: String)
    fun getToken(): String?
    fun clear()
}
