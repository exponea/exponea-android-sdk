package com.exponea.sdk.repository

import com.exponea.sdk.Exponea
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.TokenType

internal class PushTokenRepositoryImpl(private val preferences: ExponeaPreferences) : PushTokenRepository {

    // keeping the names for backward compatibility, but repo can contain HMS token as well as Firebase token
    private val key = "ExponeaFirebaseToken"
    private val keyDate = "ExponeaLastFirebaseTokenDate"
    private val keyType = "ExponeaLastTokenType"
    private val keyPermFlag = "ExponeaLastTokenPermissionGranted"
    private val keyAppVersion = "ExponeaLastNotificationStateAppVersion"
    private val keyApplicationId = "ExponeaLastNotificationStateApplicationId"

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
        preferences.setString(key, token)
        if (lastTrackDateInMilliseconds == null) {
            preferences.remove(keyDate)
        } else {
            preferences.setLong(keyDate, lastTrackDateInMilliseconds)
        }
        preferences.setString(keyType, tokenType.name)
        preferences.setBoolean(keyPermFlag, permissionGranted)
        return true
    }

    override fun clear(): Boolean {
        return preferences.remove(key) &&
            preferences.remove(keyDate) &&
            preferences.remove(keyType) &&
            preferences.remove(keyPermFlag) &&
            preferences.remove(keyAppVersion) &&
            preferences.remove(keyApplicationId)
    }

    override fun get(): String? {
        val token = preferences.getString(key, "")
        return token.ifEmpty { null }
    }

    override fun getLastTrackDateInMilliseconds(): Long? {
        val millis = preferences.getLong(keyDate, 0)
        return if (millis > 0) millis else null
    }

    override fun getLastTokenType(): TokenType {
        val type = preferences.getString(keyType, TokenType.FCM.name)
        return TokenType.valueOf(type)
    }

    override fun getLastPermissionFlag(): Boolean {
        return preferences.getBoolean(keyPermFlag, false)
    }

    override fun getLastTrackedAppVersion(): String? {
        val version = preferences.getString(keyAppVersion, "")
        return version.ifEmpty { null }
    }

    override fun setLastTrackedAppVersion(version: String) {
        preferences.setString(keyAppVersion, version)
    }

    override fun getLastTrackedApplicationId(): String? {
        val appId = preferences.getString(keyApplicationId, "")
        return appId.ifEmpty { null }
    }

    override fun setLastTrackedApplicationId(appId: String) {
        preferences.setString(keyApplicationId, appId)
    }

    override fun onIntegrationStopped() {
        clear()
    }
}
