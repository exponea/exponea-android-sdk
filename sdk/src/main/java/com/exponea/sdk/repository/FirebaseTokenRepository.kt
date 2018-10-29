package com.exponea.sdk.repository

interface FirebaseTokenRepository {
    fun get(): String?
    fun set(token: String)
    fun clear(): Boolean
}