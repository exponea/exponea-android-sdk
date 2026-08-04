package com.exponea.sdk.preferences

import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.HtmlNormalizedCacheImpl
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepositoryImpl
import com.exponea.sdk.repository.InAppMessageDisplayStateRepositoryImpl
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepositoryImpl
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl

internal object ExponeaPreferencesConstants {
    const val EXPONEA_PREFERENCES_FILE = "EXPONEA_PREFERENCES"
    const val MIGRATION_COMPLETE_KEY = "ExponeaDefaultPrefsMigrationComplete"
    const val MIGRATION_FAILURE_COUNT_KEY = "ExponeaPrefsMigrationFailureCount"
    const val MIGRATION_INCOMPLETE_COUNT_KEY = "ExponeaPrefsMigrationIncompleteCount"
    const val MIGRATION_MAX_ATTEMPTS = 3

    /** SDK keys that may still exist in default SharedPreferences and need migration to EXPONEA_PREFERENCES. */
    val KEYS_TO_MIGRATE: Set<String> = setOf(
        UniqueIdentifierRepositoryImpl.KEY,
        CustomerIdsRepositoryImpl.PREFS_CUSTOMERIDS,
        ExponeaConfigRepository.PREF_CONFIG,
        SessionManagerImpl.PREF_SESSION_START,
        SessionManagerImpl.PREF_SESSION_END,
        DeviceInitiatedRepositoryImpl.KEY,
        CampaignRepositoryImpl.KEY,
        PushNotificationRepositoryImpl.KEY_EXTRA_DATA,
        PushNotificationRepositoryImpl.KEY_DELIVERED_DATA,
        PushNotificationRepositoryImpl.KEY_CLICKED_DATA,
        InAppContentBlockDisplayStateRepositoryImpl.KEY,
        InAppMessageDisplayStateRepositoryImpl.KEY
    )

    /** Push token keys that may still exist in default SharedPreferences and need migration to EXPONEA_PUSH_TOKEN. */
    val PUSH_TOKEN_KEYS_TO_MIGRATE: Set<String> = setOf(
        PushTokenRepositoryImpl.KEY,
        PushTokenRepositoryImpl.KEY_DATE,
        PushTokenRepositoryImpl.KEY_TYPE,
        PushTokenRepositoryImpl.KEY_PERMISSION_GRANTED,
        PushTokenRepositoryImpl.KEY_APP_VERSION,
        PushTokenRepositoryImpl.KEY_APPLICATION_ID
    )

    fun isDynamicSdkKey(key: String): Boolean = HtmlNormalizedCacheImpl.isDynamicSdkKey(key)

    /** Returns true for keys that must be migrated and removed from default SharedPreferences. */
    fun isSdkKeyForMigration(key: String): Boolean =
        key in KEYS_TO_MIGRATE || key in PUSH_TOKEN_KEYS_TO_MIGRATE || isDynamicSdkKey(key)
}
