package com.exponea.sdk.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
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
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl
import com.exponea.sdk.util.TokenType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaPreferencesMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearAllPrefs()
        ExponeaPreferencesImpl.resetMigrationCache()
    }

    // region happy path

    @Test
    fun `fresh install sets migration complete flag`() {
        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        val target = dedicatedPrefs()
        assertTrue(target.getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))
        assertEquals(0, PreferenceManager.getDefaultSharedPreferences(context).all.size)
    }

    @Test
    fun `already complete skips migration and leaves legacy keys untouched`() {
        val legacy = legacyPrefs()
        legacy.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "never-migrate").commit()
        dedicatedPrefs().edit()
            .putBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, true)
            .commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertTrue(legacy.contains(UniqueIdentifierRepositoryImpl.KEY))
        ExponeaPreferencesImpl.resetMigrationCache()
        assertEquals("", ExponeaPreferencesImpl(context).getString(UniqueIdentifierRepositoryImpl.KEY, ""))
    }

    @Test
    fun `upgrade migrates all legacy default keys to dedicated file`() {
        val legacy = legacyPrefs()
        legacy.edit()
            .putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123")
            .putString(CustomerIdsRepositoryImpl.PREFS_CUSTOMERIDS, """{"registered":"user-1"}""")
            .putString(ExponeaConfigRepository.PREF_CONFIG, """{"projectToken":"tok"}""")
            .putLong(SessionManagerImpl.PREF_SESSION_START, 100L)
            .putLong(SessionManagerImpl.PREF_SESSION_END, 200L)
            .putBoolean(DeviceInitiatedRepositoryImpl.KEY, true)
            .putString(CampaignRepositoryImpl.KEY, "campaign-data")
            .putString(PushNotificationRepositoryImpl.KEY_EXTRA_DATA, "extra")
            .putString(PushNotificationRepositoryImpl.KEY_DELIVERED_DATA, "delivered")
            .putString(PushNotificationRepositoryImpl.KEY_CLICKED_DATA, "clicked")
            .putString(InAppContentBlockDisplayStateRepositoryImpl.KEY, """{"block-1":{}}""")
            .putString(InAppMessageDisplayStateRepositoryImpl.KEY, """{"message-1":{}}""")
            .commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        val prefs = ExponeaPreferencesImpl(context)
        assertEquals("cookie-123", prefs.getString(UniqueIdentifierRepositoryImpl.KEY, ""))
        assertEquals("""{"registered":"user-1"}""", prefs.getString(CustomerIdsRepositoryImpl.PREFS_CUSTOMERIDS, ""))
        assertEquals("""{"projectToken":"tok"}""", prefs.getString(ExponeaConfigRepository.PREF_CONFIG, ""))
        assertEquals(100L, prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))
        assertEquals(200L, prefs.getLong(SessionManagerImpl.PREF_SESSION_END, -1L))
        assertTrue(prefs.getBoolean(DeviceInitiatedRepositoryImpl.KEY, false))
        assertEquals("campaign-data", prefs.getString(CampaignRepositoryImpl.KEY, ""))
        assertEquals("extra", prefs.getString(PushNotificationRepositoryImpl.KEY_EXTRA_DATA, ""))
        assertEquals("delivered", prefs.getString(PushNotificationRepositoryImpl.KEY_DELIVERED_DATA, ""))
        assertEquals("clicked", prefs.getString(PushNotificationRepositoryImpl.KEY_CLICKED_DATA, ""))
        assertEquals("""{"block-1":{}}""", prefs.getString(InAppContentBlockDisplayStateRepositoryImpl.KEY, ""))
        assertEquals("""{"message-1":{}}""", prefs.getString(InAppMessageDisplayStateRepositoryImpl.KEY, ""))

        ExponeaPreferencesConstants.KEYS_TO_MIGRATE.forEach { key ->
            assertFalse(legacy.contains(key), "Legacy should not contain key: $key")
        }
    }

    @Test
    fun `upgrade migrates dynamic HTML cache keys`() {
        val hashKey = "${HtmlNormalizedCacheImpl.HASH_PREFIX}abc"
        val fileKey = "${HtmlNormalizedCacheImpl.FILE_PREFIX}abc"
        val legacy = legacyPrefs()
        legacy.edit()
            .putString(hashKey, "hash-value")
            .putString(fileKey, "file.json")
            .commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        val prefs = ExponeaPreferencesImpl(context)
        assertEquals("hash-value", prefs.getString(hashKey, ""))
        assertEquals("file.json", prefs.getString(fileKey, ""))
        assertFalse(legacy.contains(hashKey))
        assertFalse(legacy.contains(fileKey))
    }

    @Test
    fun `migration is idempotent once complete`() {
        val legacy = legacyPrefs()
        legacy.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123").commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )
        legacy.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "should-not-reappear").commit()
        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertEquals("cookie-123", ExponeaPreferencesImpl(context).getString(UniqueIdentifierRepositoryImpl.KEY, ""))
    }

    @Test
    fun `does not overwrite existing dedicated file values`() {
        val legacy = legacyPrefs()
        legacy.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "legacy-cookie").commit()
        dedicatedPrefs().edit().putString(UniqueIdentifierRepositoryImpl.KEY, "new-cookie").commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertEquals("new-cookie", ExponeaPreferencesImpl(context).getString(UniqueIdentifierRepositoryImpl.KEY, ""))
        assertFalse(legacy.contains(UniqueIdentifierRepositoryImpl.KEY))
    }

    @Test
    fun `markComplete clears migration counters from legacy`() {
        val legacy = legacyPrefs()
        legacy.edit()
            .putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123")
            .putInt(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY, 1)
            .putInt(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY, 2)
            .commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )
        assertFalse(legacy.contains(UniqueIdentifierRepositoryImpl.KEY))
        assertEquals("cookie-123", ExponeaPreferencesImpl(context).getString(UniqueIdentifierRepositoryImpl.KEY, ""))
        assertFalse(legacy.contains(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY))
        assertFalse(legacy.contains(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY))
    }

    // endregion

    // region push token

    @Test
    fun `legacy push token in default prefs migrates to EXPONEA_PUSH_TOKEN`() {
        val legacy = legacyPrefs()
        legacy.edit()
            .putString(PushTokenRepositoryImpl.KEY, "token-abc")
            .putString(PushTokenRepositoryImpl.KEY_TYPE, TokenType.FCM.name)
            .putBoolean(PushTokenRepositoryImpl.KEY_PERMISSION_GRANTED, true)
            .commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertEquals("token-abc", PushTokenRepositoryProvider.get(context).get())
        assertFalse(legacy.contains(PushTokenRepositoryImpl.KEY))
        assertFalse(legacy.contains(PushTokenRepositoryImpl.KEY_TYPE))
        assertFalse(legacy.contains(PushTokenRepositoryImpl.KEY_PERMISSION_GRANTED))
    }

    @Test
    fun `legacy push token already in dedicated file clears legacy keys`() {
        val legacy = legacyPrefs()
        legacy.edit().putString(PushTokenRepositoryImpl.KEY, "legacy-token").commit()
        PushTokenRepositoryProvider.get(context).setTrackedToken("dedicated-token", 1L, TokenType.FCM, true)

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertEquals("dedicated-token", PushTokenRepositoryProvider.get(context).get())
        assertFalse(legacy.contains(PushTokenRepositoryImpl.KEY))
    }

    @Test
    fun `legacy push keys remain when push token cannot be migrated yet`() {
        mockkObject(LegacyPushTokenMigration)
        every { LegacyPushTokenMigration.migrateIfNeeded(any()) } returns false
        every { LegacyPushTokenMigration.canRemoveLegacyPushTokenKeys(any()) } returns false

        val legacy = legacyPrefs()
        legacy.edit()
            .putString(PushTokenRepositoryImpl.KEY, "token-only-in-legacy")
            .putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123")
            .commit()

        assertEquals(
            MigrationState.PartiallyCompleted,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertEquals("cookie-123", ExponeaPreferencesImpl(context).getString(UniqueIdentifierRepositoryImpl.KEY, ""))
        assertTrue(legacy.contains(PushTokenRepositoryImpl.KEY))
        assertFalse(dedicatedPrefs().getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))

        unmockkObject(LegacyPushTokenMigration)
    }

    // endregion

    // region per-key copy failure

    @Test
    fun `unsupported key type is discarded and migration completes`() {
        val legacy = legacyPrefs()
        val unsupportedKey = "${HtmlNormalizedCacheImpl.HASH_PREFIX}unsupported"
        legacy.edit()
            .putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123")
            .putStringSet(unsupportedKey, setOf("unsupported-type"))
            .commit()

        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        val target = dedicatedPrefs()
        assertTrue(target.getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))
        assertEquals("cookie-123", target.getString(UniqueIdentifierRepositoryImpl.KEY, ""))
        assertFalse(legacy.contains(unsupportedKey))
        assertFalse(target.contains(unsupportedKey))
    }

    // endregion

    // region process cache & concurrency

    @Test
    fun `default preferences migration runs only once per process`() {
        mockkObject(ExponeaPreferencesMigration)
        every { ExponeaPreferencesMigration.resolveMigrationState(any()) } answers { callOriginal() }

        legacyPrefs().edit().putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123").commit()

        ExponeaPreferencesImpl(context)
        ExponeaPreferencesImpl(context)
        ExponeaPreferencesImpl(context)

        verify(exactly = 1) { ExponeaPreferencesMigration.resolveMigrationState(any()) }
        assertFalse(legacyPrefs().contains(UniqueIdentifierRepositoryImpl.KEY))
        assertTrue(dedicatedPrefs().contains(UniqueIdentifierRepositoryImpl.KEY))
        unmockkObject(ExponeaPreferencesMigration)
    }

    @Test
    fun `default preferences migration result is cached for process`() {
        val legacy = legacyPrefs()
        legacy.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123").commit()

        val targetFirst = ExponeaPreferencesImpl(context)
        assertEquals("cookie-123", targetFirst.getString(UniqueIdentifierRepositoryImpl.KEY, ""))

        legacy.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "new-legacy-value").commit()

        val targetSecond = ExponeaPreferencesImpl(context)
        assertEquals("cookie-123", targetSecond.getString(UniqueIdentifierRepositoryImpl.KEY, ""))
    }

    @Test
    fun `concurrent first access resolves to the same migrated preferences`() {
        val legacy = legacyPrefs()
        legacy.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123").commit()

        val threadCount = 8
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val errors = ConcurrentLinkedQueue<Throwable>()
        val cookies = ConcurrentLinkedQueue<String>()

        repeat(threadCount) {
            Thread {
                try {
                    startLatch.await(10, TimeUnit.SECONDS)
                    cookies.add(
                        ExponeaPreferencesImpl(context).getString(UniqueIdentifierRepositoryImpl.KEY, "")
                    )
                } catch (error: Throwable) {
                    errors.add(error)
                } finally {
                    doneLatch.countDown()
                }
            }.start()
        }

        startLatch.countDown()
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS))
        assertTrue(errors.isEmpty(), errors.joinToString("\n") { it.message ?: it.toString() })
        assertTrue(cookies.all { it == "cookie-123" })
        assertEquals("cookie-123", dedicatedPrefs().getString(UniqueIdentifierRepositoryImpl.KEY, ""))
        assertFalse(legacy.contains(UniqueIdentifierRepositoryImpl.KEY))
    }

    // endregion

    // region incomplete cleanup

    @Test
    fun `incomplete migration retries below threshold then scrubs on third attempt`() {
        val legacy = legacyPrefs()
        legacy.edit()
            .putString(PushTokenRepositoryImpl.KEY_TYPE, TokenType.FCM.name)
            .putBoolean(PushTokenRepositoryImpl.KEY_PERMISSION_GRANTED, true)
            .commit()

        val target = dedicatedPrefs()

        repeat(ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS - 1) { attempt ->
            ExponeaPreferencesImpl.resetMigrationCache()
            assertEquals(
                MigrationState.PartiallyCompleted,
                ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
            )
            assertFalse(target.getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))
            assertTrue(legacy.contains(PushTokenRepositoryImpl.KEY_TYPE))
            assertEquals(
                attempt + 1,
                legacy.getInt(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY, 0)
            )
        }

        ExponeaPreferencesImpl.resetMigrationCache()
        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertTrue(target.getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))
        assertFalse(legacy.contains(PushTokenRepositoryImpl.KEY_TYPE))
        assertFalse(legacy.contains(PushTokenRepositoryImpl.KEY_PERMISSION_GRANTED))
        assertFalse(legacy.contains(ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY))
    }

    @Test
    fun `incomplete terminal path marks complete even when legacy scrub fails`() {
        mockkObject(ExponeaPreferencesCleanup)
        every { ExponeaPreferencesCleanup.removeLegacySdkKeys(any<SharedPreferences>()) } returns false

        val legacy = legacyPrefs()
        legacy.edit()
            .putString(PushTokenRepositoryImpl.KEY_TYPE, TokenType.FCM.name)
            .putInt(
                ExponeaPreferencesConstants.MIGRATION_INCOMPLETE_COUNT_KEY,
                ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS - 1
            )
            .commit()

        ExponeaPreferencesImpl.resetMigrationCache()
        assertEquals(
            MigrationState.Completed,
            ExponeaPreferencesMigration.resolveMigrationState(context.applicationContext)
        )

        assertTrue(dedicatedPrefs().getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))
        unmockkObject(ExponeaPreferencesCleanup)
    }

    // endregion

    // region commit failure

    @Test
    fun `first and second commit failures increment counter and return Failed`() {
        val legacy = legacyPrefs()
        val target = dedicatedPrefs()

        val first = invokeHandleCommitFailure(target, legacy)
        assertEquals(MigrationState.Failed, first)
        assertEquals(1, legacy.getInt(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY, 0))

        val second = invokeHandleCommitFailure(target, legacy)
        assertEquals(MigrationState.Failed, second)
        assertEquals(2, legacy.getInt(ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY, 0))
        assertFalse(target.getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))
    }

    @Test
    fun `third commit failure hard resets files marks complete and opens dedicated prefs`() {
        val legacy = legacyPrefs()
        legacy.edit()
            .putString(UniqueIdentifierRepositoryImpl.KEY, "cookie-123")
            .putInt(
                ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY,
                ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS - 1
            )
            .commit()

        val target = dedicatedPrefs()
        target.edit().putString(UniqueIdentifierRepositoryImpl.KEY, "orphan-cookie").commit()

        assertEquals(MigrationState.Completed, invokeHandleCommitFailure(target, legacy))

        assertFalse(target.contains(UniqueIdentifierRepositoryImpl.KEY))
        assertTrue(target.getBoolean(ExponeaPreferencesConstants.MIGRATION_COMPLETE_KEY, false))
        assertEquals(1, target.all.size)
        assertFalse(legacy.contains(UniqueIdentifierRepositoryImpl.KEY))

        ExponeaPreferencesImpl.resetMigrationCache()
        val prefs = ExponeaPreferencesImpl(context)
        prefs.setString("post-reset-key", "post-reset-value")
        assertEquals("post-reset-value", target.getString("post-reset-key", ""))
        assertFalse(legacy.contains("post-reset-key"))
    }

    @Test
    fun `third commit failure uses ExponeaPreferencesCleanup only`() {
        mockkObject(ExponeaPreferencesCleanup)
        every { ExponeaPreferencesCleanup.clearAllSdkPreferenceFiles(any()) } just Runs

        val legacy = legacyPrefs()
        legacy.edit()
            .putInt(
                ExponeaPreferencesConstants.MIGRATION_FAILURE_COUNT_KEY,
                ExponeaPreferencesConstants.MIGRATION_MAX_ATTEMPTS - 1
            )
            .commit()

        assertEquals(MigrationState.Completed, invokeHandleCommitFailure(dedicatedPrefs(), legacy))

        verify(exactly = 1) { ExponeaPreferencesCleanup.clearAllSdkPreferenceFiles(any()) }
        unmockkObject(ExponeaPreferencesCleanup)
    }

    @Test
    fun `when migration cache is false openPreferences uses legacy file`() {
        val legacy = legacyPrefs()
        legacy.edit()
            .putString(UniqueIdentifierRepositoryImpl.KEY, "legacy-cookie")
            .commit()

        dedicatedPrefs().edit()
            .putString(UniqueIdentifierRepositoryImpl.KEY, "dedicated-copy")
            .commit()

        ExponeaPreferencesImpl.resetMigrationCache()
        setMigrationStateForTest(MigrationState.Failed)

        val prefs = ExponeaPreferencesImpl(context)
        assertEquals("legacy-cookie", prefs.getString(UniqueIdentifierRepositoryImpl.KEY, ""))

        prefs.setString("process-path-marker", "via-legacy")
        assertTrue(legacy.contains("process-path-marker"))
        assertFalse(dedicatedPrefs().contains("process-path-marker"))
    }

    // endregion

    private fun invokeHandleCommitFailure(
        target: SharedPreferences,
        legacy: SharedPreferences
    ): MigrationState {
        val method = ExponeaPreferencesMigration::class.java.getDeclaredMethod(
            "handleCommitFailure",
            Context::class.java,
            SharedPreferences::class.java,
            SharedPreferences::class.java
        )
        method.isAccessible = true
        return method.invoke(
            ExponeaPreferencesMigration,
            context.applicationContext,
            target,
            legacy
        ) as MigrationState
    }

    private fun setMigrationStateForTest(state: MigrationState) {
        val field = ExponeaPreferencesImpl::class.java.getDeclaredField("migrationState")
        field.isAccessible = true
        field.set(null, state)
    }

    private fun legacyPrefs(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun dedicatedPrefs(): SharedPreferences =
        context.getSharedPreferences(
            ExponeaPreferencesConstants.EXPONEA_PREFERENCES_FILE,
            Context.MODE_PRIVATE
        )

    private fun clearAllPrefs() {
        legacyPrefs().edit().clear().commit()
        dedicatedPrefs().edit().clear().commit()
        context.getSharedPreferences("EXPONEA_PUSH_TOKEN", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
