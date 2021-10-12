package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.TokenType

internal interface PushTokenRepository {
    fun get(): String?
    fun getLastTrackDateInMilliseconds(): Long?
    fun set(token: String, lastTrackDateInMilliseconds: Long, tokenType: TokenType)
    fun clear(): Boolean
    fun getLastTokenType(): TokenType
}

internal object PushTokenRepositoryProvider {
    fun get(context: Context): PushTokenRepositoryImpl {
        return PushTokenRepositoryImpl(ExponeaPreferencesImpl(context))
    }
}
