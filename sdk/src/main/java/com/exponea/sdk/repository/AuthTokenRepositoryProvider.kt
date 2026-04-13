package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.KeystoreEncryptionManager

/**
 * Holds a single shared [AuthTokenRepository] instance.
 */
internal object AuthTokenRepositoryProvider {

    private const val AUTH_PREFERENCES_FILE_NAME = "EXPONEA_AUTH"

    @Volatile
    private var instance: AuthTokenRepository? = null

    fun get(context: Context): AuthTokenRepository {
        return instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val prefs = ExponeaPreferencesImpl(appContext, AUTH_PREFERENCES_FILE_NAME)
                val encryption = KeystoreEncryptionManager(appContext, prefs)
                AuthTokenRepositoryImpl(prefs, encryption)
            }.also { instance = it }
        }
    }

    fun clear() {
        synchronized(this) {
            instance?.clear()
            instance = null
        }
    }
}
