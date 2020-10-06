package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.preferences.ExponeaPreferencesImpl

internal interface FirebaseTokenRepository {
    fun get(): String?
    fun getLastTrackDateInMilliseconds(): Long?
    fun set(token: String, lastTrackDateInMilliseconds: Long)
    fun clear(): Boolean
}

internal object FirebaseTokenRepositoryProvider {
    fun get(context: Context): FirebaseTokenRepositoryImpl {
        return FirebaseTokenRepositoryImpl(ExponeaPreferencesImpl(context))
    }
}
