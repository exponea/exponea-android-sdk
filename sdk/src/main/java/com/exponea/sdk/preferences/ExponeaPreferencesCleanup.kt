package com.exponea.sdk.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.exponea.sdk.util.Logger

internal object ExponeaPreferencesCleanup {

    /**
     * Clears SDK customer data from both [ExponeaPreferencesConstants.EXPONEA_PREFERENCES_FILE]
     * and legacy default SharedPreferences.
     */
    fun clearAllSdkPreferenceFiles(context: Context) {
        val appContext = context.applicationContext
        if (!clearDedicatedPreferences(appContext)) {
            Logger.e(this, "Failed to clear dedicated Exponea preferences file")
        }
        if (!removeLegacySdkKeys(appContext)) {
            Logger.e(this, "Failed to remove SDK keys from legacy default preferences")
        }
        ExponeaPreferencesImpl.resetMigrationCache()
    }

    fun clearDedicatedPreferences(context: Context): Boolean {
        val dedicated = context.applicationContext.getSharedPreferences(
            ExponeaPreferencesConstants.EXPONEA_PREFERENCES_FILE,
            Context.MODE_PRIVATE
        )
        return dedicated.edit().clear().commit()
    }

    fun removeLegacySdkKeys(context: Context): Boolean {
        return removeLegacySdkKeys(
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        )
    }

    fun removeLegacySdkKeys(legacy: SharedPreferences): Boolean {
        val editor = legacy.edit()
        legacy.all.keys
            .filter { ExponeaPreferencesConstants.isSdkKeyForMigration(it) }
            .forEach { editor.remove(it) }
        editor.remove(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY)
        editor.remove(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY)
        return editor.commit()
    }
}
