package com.exponea.sdk.repository

import com.exponea.sdk.Exponea
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.TokenType

internal class PushTokenRepositoryImpl(private val preferences: ExponeaPreferences) : PushTokenRepository {

    companion object {
        // keeping the names for backward compatibility, but repo can contain HMS token as well as Firebase token
        internal const val KEY = "ExponeaFirebaseToken"
        internal const val KEY_DATE = "ExponeaLastFirebaseTokenDate"
        internal const val KEY_TYPE = "ExponeaLastTokenType"
        internal const val KEY_PERMISSION_GRANTED = "ExponeaLastTokenPermissionGranted"
        internal const val KEY_APP_VERSION = "ExponeaLastNotificationStateAppVersion"
        internal const val KEY_APPLICATION_ID = "ExponeaLastNotificationStateApplicationId"
    }

    override fun setTrackedToken(
        token: String,
        lastTrackDateInMilliseconds: Long,
        tokenType: TokenType,
        permissionGranted: Boolean
    ) {
        storeTokenInternal(token, lastTrackDateInMilliseconds, tokenType, permissionGranted)
    }

    override fun setUntrackedToken(token: String, tokenType: TokenType, permissionGranted: Boolean) {
        storeTokenInternal(token, null, tokenType, permissionGranted)
    }

    internal fun setTrackedTokenForce(
        token: String,
        lastTrackDateInMilliseconds: Long?,
        tokenType: TokenType,
        permissionGranted: Boolean
    ): Boolean {
        return storeTokenInternal(
            token,
            lastTrackDateInMilliseconds,
            tokenType,
            permissionGranted,
            ignoreStop = true
        )
    }

    private fun storeTokenInternal(
        token: String,
        lastTrackDateInMilliseconds: Long?,
        tokenType: TokenType,
        permissionGranted: Boolean,
        ignoreStop: Boolean = false
    ): Boolean {
        if (Exponea.isStopped && !ignoreStop) {
            Logger.e(this, "Push token not stored, SDK is stopping")
            return false
        }
        preferences.setString(KEY, token)
        if (lastTrackDateInMilliseconds == null) {
            preferences.remove(KEY_DATE)
        } else {
            preferences.setLong(KEY_DATE, lastTrackDateInMilliseconds)
        }
        preferences.setString(KEY_TYPE, tokenType.name)
        preferences.setBoolean(KEY_PERMISSION_GRANTED, permissionGranted)
        return true
    }

    override fun clear(): Boolean {
        return preferences.remove(KEY) &&
            preferences.remove(KEY_DATE) &&
            preferences.remove(KEY_TYPE) &&
            preferences.remove(KEY_PERMISSION_GRANTED) &&
            preferences.remove(KEY_APP_VERSION) &&
            preferences.remove(KEY_APPLICATION_ID)
    }

    override fun get(): String? {
        val token = preferences.getString(KEY, "")
        return token.ifEmpty { null }
    }

    override fun getLastTrackDateInMilliseconds(): Long? {
        val millis = preferences.getLong(KEY_DATE, 0)
        return if (millis > 0) millis else null
    }

    override fun getLastTokenType(): TokenType {
        val type = preferences.getString(KEY_TYPE, TokenType.FCM.name)
        return TokenType.valueOf(type)
    }

    override fun getLastPermissionFlag(): Boolean {
        return preferences.getBoolean(KEY_PERMISSION_GRANTED, false)
    }

    override fun getLastTrackedAppVersion(): String? {
        val version = preferences.getString(KEY_APP_VERSION, "")
        return version.ifEmpty { null }
    }

    override fun setLastTrackedAppVersion(version: String) {
        preferences.setString(KEY_APP_VERSION, version)
    }

    override fun getLastTrackedApplicationId(): String? {
        val appId = preferences.getString(KEY_APPLICATION_ID, "")
        return appId.ifEmpty { null }
    }

    override fun setLastTrackedApplicationId(appId: String) {
        preferences.setString(KEY_APPLICATION_ID, appId)
    }

    override fun onIntegrationStopped() {
        clear()
    }
}
