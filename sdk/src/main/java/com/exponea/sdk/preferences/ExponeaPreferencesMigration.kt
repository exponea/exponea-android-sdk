package com.exponea.sdk.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.exponea.sdk.util.Logger

internal enum class MigrationState {
    /** Dedicated shared preferences EXPONEA_PREFERENCES is ready to use. */
    Completed,

    /** Transient failure, caller should keep using default SharedPreferences until the next attempt. */
    Failed,

    /** Migration ran but legacy keys remain, retry on next cold start. Dedicated file is still used. */
    PartiallyCompleted;

    val useDedicatedPreferences: Boolean
        get() = this != Failed
}

internal object ExponeaPreferencesMigration {

    private val lock = Any()

    /**
     * Runs shared preferences migration when needed and returns the resulting state.
     */
    fun resolveMigrationState(context: Context): MigrationState {
        val appContext = context.applicationContext
        synchronized(lock) {
            val target = appContext.getSharedPreferences(
                ExponeaPreferencesConstants.EXPONEA_PREFERENCES_FILE,
                Context.MODE_PRIVATE
            )

            if (target.getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false)) {
                return MigrationState.Completed
            }

            val legacy = PreferenceManager.getDefaultSharedPreferences(appContext)

            LegacyPushTokenMigration.migrateIfNeeded(appContext)

            val keysToMigrate = buildKeySet(legacy)
            if (keysToMigrate.isEmpty()) {
                return finishWithoutMigration(appContext, target, legacy)
            }

            val targetEditor = target.edit()
            val legacyEditor = legacy.edit()

            keysToMigrate.forEach { key ->
                migrateKey(legacy, target, targetEditor, legacyEditor, key)
            }

            if (LegacyPushTokenMigration.canRemoveLegacyPushTokenKeys(appContext)) {
                removeLegacyPushTokenKeys(legacyEditor)
            }

            if (targetEditor.commit()) {
                Logger.d(this, "Migration - target prefs commit succeeded")
            } else {
                Logger.e(this, "Migration - target prefs commit failed")
                return handleCommitFailure(appContext, target, legacy)
            }

            if (legacyEditor.commit()) {
                Logger.d(this, "Migration - legacy prefs commit succeeded")
            } else {
                Logger.e(this, "Migration - legacy prefs commit failed")
                return handleCommitFailure(appContext, target, legacy)
            }

            return checkRemovedKeysAndComplete(target, legacy)
        }
    }

    private fun checkRemovedKeysAndComplete(
        target: SharedPreferences,
        legacy: SharedPreferences
    ): MigrationState {
        // Check if all keys were removed
        if (!hasLegacySdkKeys(legacy)) {
            markComplete(target, legacy)
            return MigrationState.Completed
        }

        val incompleteCount = incrementIncompleteCount(legacy)
        if (incompleteCount < ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS) {
            Logger.w(
                this,
                "Migration - SDK keys remaining after attempt $incompleteCount of " +
                    "${ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS}, " +
                    "will retry on next cold start"
            )
            return MigrationState.PartiallyCompleted
        }

        Logger.e(
            this,
            "Migration - max attempts reached ($incompleteCount of " +
                "${ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS}), " +
                "removing SDK keys"
        )
        if (!ExponeaPreferencesCleanup.removeLegacySdkKeys(legacy)) {
            Logger.e(
                this,
                "Migration - could not remove remaining SDK keys from legacy default preferences " +
                    "after $incompleteCount attempts"
            )
        }
        markComplete(target, legacy)
        return MigrationState.Completed
    }

    private fun finishWithoutMigration(
        context: Context,
        target: SharedPreferences,
        legacy: SharedPreferences
    ): MigrationState {
        if (LegacyPushTokenMigration.canRemoveLegacyPushTokenKeys(context)) {
            if (!removeLegacyPushTokenKeys(legacy)) {
                Logger.e(this, "Migration - legacy push token keys commit failed")
                return handleCommitFailure(context.applicationContext, target, legacy)
            }
        }
        return checkRemovedKeysAndComplete(target, legacy)
    }

    private fun migrateKey(
        legacy: SharedPreferences,
        target: SharedPreferences,
        targetEditor: SharedPreferences.Editor,
        legacyEditor: SharedPreferences.Editor,
        key: String
    ) {
        if (!legacy.contains(key)) {
            return
        }
        if (!target.contains(key)) {
            if (!copyKey(legacy, targetEditor, key)) {
                Logger.e(this, "Migration - failed to copy key $key — discarding from legacy")
            }
        }
        // Always remove from legacy even when copy failed so a partial migration cannot leave
        // SDK keys in default prefs, otherwise the next run could mix stale legacy values with new data in dedicated.
        legacyEditor.remove(key)
    }

    private fun copyKey(
        legacy: SharedPreferences,
        targetEditor: SharedPreferences.Editor,
        key: String
    ): Boolean = runCatching {
        when (val value = legacy.all[key]) {
            is String -> targetEditor.putString(key, value)
            is Boolean -> targetEditor.putBoolean(key, value)
            is Int -> targetEditor.putInt(key, value)
            is Long -> targetEditor.putLong(key, value)
            is Float -> targetEditor.putFloat(key, value)
            null -> return true
            else -> {
                Logger.e(this, "Migration - unsupported type for key $key: ${value.javaClass}")
                return false
            }
        }
        true
    }.getOrElse {
        Logger.e(this, "Migration - failed to copy key $key", it)
        false
    }

    private fun buildKeySet(legacy: SharedPreferences): List<String> {
        val dynamic = legacy.all.keys.filter { ExponeaPreferencesConstants.isDynamicSdkKey(it) }
        val fixed = ExponeaPreferencesConstants.KEYS_TO_MIGRATE.filter { legacy.contains(it) }
        return fixed + dynamic
    }

    private fun hasLegacySdkKeys(legacy: SharedPreferences): Boolean {
        return legacy.all.keys.any { ExponeaPreferencesConstants.isSdkKeyForMigration(it) }
    }

    private fun removeLegacyPushTokenKeys(legacy: SharedPreferences): Boolean {
        val editor = legacy.edit()
        removeLegacyPushTokenKeys(editor)
        return editor.commit()
    }

    private fun removeLegacyPushTokenKeys(legacyEditor: SharedPreferences.Editor) {
        ExponeaPreferencesConstants.PUSH_TOKEN_KEYS_TO_MIGRATE.forEach { key ->
            legacyEditor.remove(key)
        }
    }

    private fun handleCommitFailure(
        context: Context,
        target: SharedPreferences,
        legacy: SharedPreferences
    ): MigrationState {
        val failureCount = incrementFailureCount(legacy)
        if (failureCount >= ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS) {
            Logger.e(
                this,
                "Migration - commit failed $failureCount times (max " +
                    "${ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS}), " +
                    "clearing SDK preference files"
            )
            // After repeated commit I/O failures, wipe legacy default + dedicated prefs
            // and mark migration complete, and use empty EXPONEA_PREFERENCES
            ExponeaPreferencesCleanup.clearAllSdkPreferenceFiles(context)
            markComplete(target, legacy)
            return MigrationState.Completed
        }
        Logger.w(
            this,
            "Migration - commit failed (attempt $failureCount of " +
                "${ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS}), " +
                "will retry on next cold start"
        )
        return MigrationState.Failed
    }

    private fun incrementIncompleteCount(legacy: SharedPreferences): Int {
        val count = legacy.getInt(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY, 0) + 1
        legacy.edit(commit = true) {
            putInt(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY, count)
        }
        return count
    }

    private fun incrementFailureCount(legacy: SharedPreferences): Int {
        val count = legacy.getInt(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY, 0) + 1
        legacy.edit(commit = true) {
            putInt(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY, count)
        }
        return count
    }

    private fun markComplete(target: SharedPreferences, legacy: SharedPreferences) {
        target.edit(commit = true) {
            putBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, true)
        }
        legacy.edit(commit = true) {
            remove(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY)
            remove(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY)
        }
        Logger.i(this, "Migration - marked complete")
    }
}
