package com.exponea.sdk.preferences

interface ExponeaPreferences {
    fun setString(key: String, value: String)
    fun setBoolean(key: String, value: Boolean)
    fun setLong(key: String, value: Long)
    fun setFloat(key: String, value: Float)

    fun getString(key: String, default: String): String
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getLong(key: String, default: Long): Long
    fun getFloat(key: String, default: Float) : Float

    fun remove(key: String): Boolean
}