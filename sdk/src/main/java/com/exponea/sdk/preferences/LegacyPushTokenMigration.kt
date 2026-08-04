package com.exponea.sdk.preferences

import android.content.Context
import android.preference.PreferenceManager
import com.exponea.sdk.repository.PushTokenRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.util.Logger

internal object LegacyPushTokenMigration {

    private val lock = Any()

    /**
     * Migrates push token data from legacy default SharedPreferences to EXPONEA_PUSH_TOKEN.
     * Serialized with [com.exponea.sdk.receiver.AppUpdateReceiver] and [ExponeaPreferencesMigration] callers.
     *
     * @return true when legacy push storage can be cleared or migration to dedicated file succeeded
     */
    fun migrateIfNeeded(context: Context): Boolean {
        synchronized(lock) {
            return migrateIfNeededLocked(context)
        }
    }

    /**
     * Returns true when legacy default prefs no longer need push token keys.
     */
    fun canRemoveLegacyPushTokenKeys(context: Context): Boolean {
        synchronized(lock) {
            return canRemoveLegacyPushTokenKeysLocked(context)
        }
    }

    private fun migrateIfNeededLocked(context: Context): Boolean {
        val obsoleteRepo = PushTokenRepositoryImpl(ExponeaPreferencesImpl.forLegacyDefault(context))
        val token = obsoleteRepo.get()
        if (token.isNullOrEmpty()) {
            Logger.d(this, "Legacy push token migration not needed")
            return true
        }
        val newRepo = PushTokenRepositoryProvider.get(context)
        if (!newRepo.get().isNullOrEmpty()) {
            Logger.d(this, "Legacy push token migration already done")
            obsoleteRepo.clear()
            return true
        }
        return if (newRepo.setTrackedTokenForce(
                token,
                /* setting time to null to force resending notification_state event after migration
                If the time is not reset to null, the notification_state would be sent only after a token renewal which
                might create a considerable delay in delivering notification_state event
                */
                null,
                obsoleteRepo.getLastTokenType(),
                obsoleteRepo.getLastPermissionFlag()
            )) {
            obsoleteRepo.clear()
            Logger.d(this, "Legacy push token migration completed")
            true
        } else {
            Logger.e(this, "Legacy push token migration failed")
            false
        }
    }

    private fun canRemoveLegacyPushTokenKeysLocked(context: Context): Boolean {
        val legacy = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val hasLegacyPushKeys = ExponeaPreferencesConstants.PUSH_TOKEN_KEYS_TO_MIGRATE.any { legacy.contains(it) }
        if (!hasLegacyPushKeys) {
            return true
        }
        if (!PushTokenRepositoryProvider.get(context).get().isNullOrEmpty()) {
            return true
        }
        return false
    }
}
