package com.exponea.sdk.repository

interface FirebaseTokenRepository {
    fun get(): String?
    fun getLastTrackDateInMilliseconds(): Long?
    fun set(token: String, lastTrackDateInMilliseconds: Long)
    fun clear(): Boolean
}
