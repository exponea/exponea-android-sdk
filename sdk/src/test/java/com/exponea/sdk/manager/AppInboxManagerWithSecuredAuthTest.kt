package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.AppInboxCache
import com.exponea.sdk.repository.AppInboxCacheImpl
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.services.AuthorizationProvider
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import com.google.gson.Gson
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
internal class AppInboxManagerWithSecuredAuthTest : ExponeaSDKTest() {

    private lateinit var appInboxManager: AppInboxManager
    private lateinit var appInboxCache: AppInboxCache
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var bitmapCache: InAppMessageBitmapCache
    private lateinit var fetchManager: FetchManager

    @Before
    fun before() {
        fetchManager = mockk()
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any()) } just Runs
        bitmapCache = mockk()
        every { bitmapCache.has(any()) } returns false
        every { bitmapCache.preload(any(), any()) } just Runs
        every { bitmapCache.clearExcept(any()) } just Runs
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds()
        appInboxCache = AppInboxCacheImpl(
            ApplicationProvider.getApplicationContext(), Gson()
        )
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should use basic auth`() {
        val projectWithAuth = slot<ExponeaProject>()
        prepareAuth(basic = "mock-auth-basic")
        appInboxManager.fetchAppInbox { }
        verify(exactly = 1) {
            fetchManager.fetchAppInbox(capture(projectWithAuth), any(), any(), any(), any())
        }
        assertEquals("Token mock-auth-basic", projectWithAuth.captured.authorization)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should use auth from provider`() {
        val projectWithAuth = slot<ExponeaProject>()
        TestSecureAuthProvider.token = "mock-auth-secured-by-provider"
        prepareAuth(provider = TestSecureAuthProvider::class)
        appInboxManager.fetchAppInbox { }
        verify(exactly = 1) {
            fetchManager.fetchAppInbox(capture(projectWithAuth), any(), any(), any(), any())
        }
        assertEquals("Bearer mock-auth-secured-by-provider", projectWithAuth.captured.authorization)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should prioritize auth from provider than basic`() {
        val projectWithAuth = slot<ExponeaProject>()
        TestSecureAuthProvider.token = "mock-auth-secured-by-provider"
        prepareAuth(
            basic = "mock-auth-basic",
            provider = TestSecureAuthProvider::class
        )
        appInboxManager.fetchAppInbox { }
        verify(exactly = 1) {
            fetchManager.fetchAppInbox(capture(projectWithAuth), any(), any(), any(), any())
        }
        assertEquals("Bearer mock-auth-secured-by-provider", projectWithAuth.captured.authorization)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should failed with defined auth provider without token`() {
        TestSecureAuthProvider.token = null
        prepareAuth(
            provider = TestSecureAuthProvider::class
        )
        waitForIt(2000) { done ->
            appInboxManager.fetchAppInbox {
                assertNull(it)
                done()
            }
        }
        verify(exactly = 0) {
            fetchManager.fetchAppInbox(any(), any(), any(), any(), any())
        }
    }

    private fun prepareAuth(
        basic: String? = null,
        provider: KClass<out AuthorizationProvider>? = null
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        val configuration = ExponeaConfiguration(
            baseURL = "https://base-url.com",
            projectToken = "project_token",
            authorization = "Token " + (basic ?: "mock-auth"),
            advancedAuthEnabled = provider != null
        )
        val projectFactory = object : ExponeaProjectFactory(context, configuration) {
            override fun readAuthorizationProviderName(context: Context): String? {
                return provider?.qualifiedName
            }
        }
        appInboxManager = AppInboxManagerImpl(
            fetchManager = fetchManager,
            bitmapCache = bitmapCache,
            customerIdsRepository = customerIdsRepository,
            appInboxCache = appInboxCache,
            projectFactory = projectFactory
        )
    }
}

class TestSecureAuthProvider : AuthorizationProvider {
    companion object {
        var token: String? = null
    }
    override fun getAuthorizationToken(): String? {
        return token
    }
}
