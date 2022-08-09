package com.exponea.sdk.services

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.receiver.AppUpdateReceiver
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.mockkConstructor
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TokenMigrationTest() : ExponeaSDKTest() {

    private val token_repo_key = "ExponeaFirebaseToken"
    private val token_1 = "ABCD_token_1"
    private val token_repo_name = "EXPONEA_PUSH_TOKEN"

    lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkConstructor(EventManagerImpl::class)
        Exponea.flushMode = FlushMode.MANUAL
    }

    @Test
    fun `should have no token after install`() {
        initExponea()
        assertNull(PushTokenRepositoryProvider.get(context).get())
    }

    @Test
    fun `should migrate token after update`() {
        val obsoleteStorage = ExponeaPreferencesImpl(context)
        obsoleteStorage.setString(token_repo_key, token_1)
        initExponea()
        simulateAppUpdate()
        assertEquals(token_1, PushTokenRepositoryProvider.get(context).get())
    }

    @Test
    fun `should not migrate after update`() {
        initExponea()
        simulateAppUpdate()
        assertNull(PushTokenRepositoryProvider.get(context).get())
    }

    private fun simulateAppUpdate() {
        val updateIntent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
        AppUpdateReceiver().onReceive(context, updateIntent)
    }

    @Test
    fun `should have no token after reinstall`() {
        Exponea.handleNewToken(context, token_1)
        assertEquals(token_1, PushTokenRepositoryProvider.get(context).get())
        simulateUninstall()
        initExponea()
        assertNull(PushTokenRepositoryProvider.get(context).get())
    }

    private fun simulateUninstall() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        context.getSharedPreferences(token_repo_name, 0).edit().clear().commit()
    }

    @Test
    fun `should have token after future update`() {
        Exponea.handleNewToken(context, token_1)
        assertEquals(token_1, PushTokenRepositoryProvider.get(context).get())
        // simulate future updates == app will use only new storage
        simulateAppUpdate()
        assertEquals(token_1, PushTokenRepositoryProvider.get(context).get())
    }

    private fun initExponea() {
        val configuration = ExponeaConfiguration()
        configuration.automaticPushNotification = false
        Exponea.init(context, configuration)
    }
}
