package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.util.TokenType

internal interface PushTokenRepository : OnIntegrationStoppedCallback {
    fun get(): String?
    fun getLastTrackDateInMilliseconds(): Long?
    fun setTrackedToken(
        token: String,
        lastTrackDateInMilliseconds: Long,
        tokenType: TokenType,
        permissionGranted: Boolean
    )
    fun setUntrackedToken(token: String, tokenType: TokenType, permissionGranted: Boolean)
    fun clear(): Boolean
    fun getLastTokenType(): TokenType
    fun getLastPermissionFlag(): Boolean
    override fun onIntegrationStopped()
}

internal object PushTokenRepositoryProvider {
    fun get(context: Context): PushTokenRepositoryImpl {
        return PushTokenRepositoryImpl(ExponeaPreferencesImpl(context, "EXPONEA_PUSH_TOKEN"))
    }
}
