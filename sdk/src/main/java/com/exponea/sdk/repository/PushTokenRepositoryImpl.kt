package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.TokenType

internal class PushTokenRepositoryImpl(private val preferences: ExponeaPreferences) : PushTokenRepository {

    // keeping the names for backward compatibility, but repo can contain HMS token as well as Firebase token
    private val key = "ExponeaFirebaseToken"
    private val keyDate = "ExponeaLastFirebaseTokenDate"
    private val keyType = "ExponeaLastTokenType"

    override fun set(token: String, lastTrackDateInMilliseconds: Long, tokenType: TokenType) {
        preferences.setString(key, token)
        preferences.setLong(keyDate, lastTrackDateInMilliseconds)
        preferences.setString(keyType, tokenType.name)
    }

    override fun clear(): Boolean {
        return preferences.remove(key) && preferences.remove(keyDate) && preferences.remove(keyType)
    }

    override fun get(): String? {
        val token = preferences.getString(key, "")
        return if (token.isNotEmpty()) token else null
    }

    override fun getLastTrackDateInMilliseconds(): Long? {
        val millis = preferences.getLong(keyDate, 0)
        return if (millis > 0) millis else null
    }

    override fun getLastTokenType(): TokenType {
        val type = preferences.getString(keyType, TokenType.FCM.name)
        return TokenType.valueOf(type)
    }
}
