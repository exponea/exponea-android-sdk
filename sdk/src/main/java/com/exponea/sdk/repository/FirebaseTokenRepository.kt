package com.exponea.sdk.repository

internal interface FirebaseTokenRepository {
    fun get(): String?
    fun getLastTrackDateInMilliseconds(): Long?
    fun set(token: String, lastTrackDateInMilliseconds: Long)
    fun clear(): Boolean
}