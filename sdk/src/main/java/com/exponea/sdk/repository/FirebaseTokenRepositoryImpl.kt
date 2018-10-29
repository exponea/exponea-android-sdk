package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences

class FirebaseTokenRepositoryImpl(private val preferences: ExponeaPreferences) : FirebaseTokenRepository {

    private val key = "ExponeaFirebaseToken"

    override fun set(token: String) {
        preferences.setString(key, token)
    }

    override fun clear(): Boolean {
        return preferences.remove(key)
    }

    override fun get(): String? {
        val token = preferences.getString(key, "")
        return if (token.isNotEmpty()) token else null
    }

}