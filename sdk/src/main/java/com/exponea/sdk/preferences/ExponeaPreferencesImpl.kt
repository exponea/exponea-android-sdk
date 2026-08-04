package com.exponea.sdk.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.exponea.sdk.util.Logger

internal class ExponeaPreferencesImpl private constructor(
    private val sharedPreferences: SharedPreferences
) : ExponeaPreferences {

    constructor(context: Context, prefsName: String? = null) : this(
        openPreferences(context.applicationContext, prefsName)
    )

    override fun setString(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    override fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    override fun setLong(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }

    override fun getString(key: String, default: String): String {
        return sharedPreferences.getString(key, default) ?: default
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    override fun getLong(key: String, default: Long): Long {
        return sharedPreferences.getLong(key, default)
    }

    override fun setDouble(key: String, value: Double) {
        Logger.d(this, "put double: $value")
        sharedPreferences.edit { putLong(key, value.toRawBits()) }
    }

    override fun getDouble(key: String, default: Double): Double {
        Logger.d(this, "get double: ${Double.fromBits(getLong(key, (-1.0).toRawBits()))}")
        return Double.fromBits(sharedPreferences.getLong(key, (-1.0).toRawBits()))
    }

    override fun remove(key: String): Boolean {
        sharedPreferences.edit { remove(key) }
        return true
    }

    override fun removeKeysWithPrefix(prefix: String) {
        sharedPreferences.edit {
            sharedPreferences.all.keys
                .filter { it.startsWith(prefix) }
                .forEach { remove(it) }
        }
    }

    internal companion object {

        private val migrationLock = Any()

        private var migrationState: MigrationState? = null

        internal fun resetMigrationCache() {
            synchronized(migrationLock) {
                migrationState = null
            }
        }

        fun forLegacyDefault(context: Context): ExponeaPreferencesImpl {
            return ExponeaPreferencesImpl(
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            )
        }

        private fun openPreferences(appContext: Context, prefsName: String?): SharedPreferences {
            prefsName?.let {
                return appContext.getSharedPreferences(it, Context.MODE_PRIVATE)
            }

            synchronized(migrationLock) {
                val state = migrationState
                    ?: ExponeaPreferencesMigration.resolveMigrationState(appContext)
                        .also { migrationState = it }

                return if (state.useDedicatedPreferences) {
                    // Dedicated EXPONEA_PREFERENCES
                    appContext.getSharedPreferences(
                        ExponeaPreferencesConstants.EXPONEA_PREFERENCES_FILE,
                        Context.MODE_PRIVATE
                    )
                } else {
                    // Fallback to default SharedPreferences when migration failed transiently
                    PreferenceManager.getDefaultSharedPreferences(appContext)
                }
            }
        }
    }
}
