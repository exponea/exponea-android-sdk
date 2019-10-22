package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences

internal class FirebaseTokenRepositoryImpl(private val preferences: ExponeaPreferences) : FirebaseTokenRepository {

    private val key = "ExponeaFirebaseToken"
    private val keyDate = "ExponeaLastFirebaseTokenDate"

    override fun set(token: String, lastTrackDateInMilliseconds: Long) {
        preferences.setString(key, token)
        preferences.setLong(keyDate, lastTrackDateInMilliseconds)
    }

    override fun clear(): Boolean {
        return preferences.remove(key) && preferences.remove(keyDate)
    }

    override fun get(): String? {
        val token = preferences.getString(key, "")
        return if (token.isNotEmpty()) token else null
    }

    override fun getLastTrackDateInMilliseconds(): Long? {
        val millis = preferences.getLong(keyDate, 0)
        return if (millis > 0) millis else null
    }
}
