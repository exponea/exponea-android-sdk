package com.exponea.sdk.preferences

interface ExponeaPreferences {
    fun setString(key: String, value: String)
    fun setBoolean(key: String, value: Boolean)

    fun getString(key: String, default: String): String
    fun getBoolean(key: String, default: Boolean): Boolean

    fun remove(key: String): Boolean
}