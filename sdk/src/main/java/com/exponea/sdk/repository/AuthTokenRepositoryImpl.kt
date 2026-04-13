package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.KeystoreEncryptionManager
import com.exponea.sdk.util.Logger

internal class AuthTokenRepositoryImpl(
    private val prefs: ExponeaPreferences,
    private val encryption: KeystoreEncryptionManager
) : AuthTokenRepository {

    companion object {
        private const val KEY_TOKEN = "exponea_auth_token"
        private const val KEY_ENCRYPTED = "exponea_auth_token_encrypted"
    }

    // Disk deferred until the first getToken() call so that configurations
    // that never use auth tokens (e.g. ProjectConfig) pay no initialization cost.
    private var loaded = false

    private var authToken: String? = null

    @Synchronized
    override fun setToken(token: String) {
        authToken = token
        loaded = true
        persistToken(token)
    }

    @Synchronized
    override fun getToken(): String? {
        if (!loaded) {
            authToken = loadFromPrefs()
            loaded = true
        }
        return authToken
    }

    @Synchronized
    override fun clear() {
        authToken = null
        loaded = true
        prefs.remove(KEY_TOKEN)
        prefs.remove(KEY_ENCRYPTED)
    }

    private fun persistToken(token: String) {
        val encrypted = encryption.encrypt(token)

        if (encrypted != null) {
            prefs.setString(KEY_TOKEN, encrypted)
            prefs.setBoolean(KEY_ENCRYPTED, true)
        } else {
            Logger.w(this, "Keystore encryption unavailable, storing token without encryption")
            prefs.setString(KEY_TOKEN, token)
            prefs.setBoolean(KEY_ENCRYPTED, false)
        }
    }

    private fun loadFromPrefs(): String? {
        val storedToken = prefs.getString(KEY_TOKEN, "")
        val isEncrypted = prefs.getBoolean(KEY_ENCRYPTED, false)

        return when {
            storedToken.isEmpty() -> null
            !isEncrypted -> storedToken
            else -> {
                val decrypted = encryption.decrypt(storedToken)
                if (decrypted == null) {
                    Logger.w(this, "Failed to decrypt persisted token, clearing stored data")
                    encryption.deleteKey()
                    prefs.remove(KEY_TOKEN)
                    prefs.remove(KEY_ENCRYPTED)
                }
                decrypted
            }
        }
    }
}
