package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences
import java.util.UUID

internal class UniqueIdentifierRepositoryImpl(
    private val preferences: ExponeaPreferences
) : UniqueIdentifierRepository {

    companion object {
        internal const val key = "ExponeaUniqueIdentifierToken"
    }

    override fun get(): String {
        var token = preferences.getString(key, "")

        if (token.isEmpty()) {
            token = UUID.randomUUID().toString()
            preferences.setString(key, token)
        }

        return token
    }

    override fun clear(): Boolean {
        return preferences.remove(key)
    }
}
