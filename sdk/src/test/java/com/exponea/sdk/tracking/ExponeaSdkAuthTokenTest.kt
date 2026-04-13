package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.repository.AuthTokenRepositoryImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaSdkAuthTokenTest : ExponeaSDKTest() {

    @Before
    fun before() {
        skipInstallEvent()
    }

    @Test
    fun `should set SDK auth token for stream integration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            integrationConfig = StreamConfig(streamId = "mock-stream-id"),
            automaticSessionTracking = false
        )
        Exponea.flushMode = FlushMode.MANUAL
        mockkConstructorFix(AuthTokenRepositoryImpl::class)

        val tokenSlot = slot<String>()
        every {
            anyConstructed<AuthTokenRepositoryImpl>().setToken(capture(tokenSlot))
        } just Runs

        Exponea.init(context, configuration)

        val authToken = "test-auth-token"
        Exponea.setSdkAuthToken(authToken)

        verify(exactly = 1) {
            anyConstructed<AuthTokenRepositoryImpl>().setToken(authToken)
        }
        assertEquals(authToken, tokenSlot.captured)
    }

    @Test
    fun `should not set SDK auth token for project integration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            integrationConfig = ProjectConfig(projectToken = "mock-token"),
            automaticSessionTracking = false
        )
        Exponea.flushMode = FlushMode.MANUAL
        mockkConstructorFix(AuthTokenRepositoryImpl::class)

        every {
            anyConstructed<AuthTokenRepositoryImpl>().setToken(any())
        } just Runs

        mockkObject(Logger)
        val logMessageSlot = slot<String>()
        every { Logger.w(any(), capture(logMessageSlot)) } returns Unit

        Exponea.init(context, configuration)

        Exponea.setSdkAuthToken("test-auth-token")

        verify(exactly = 0) {
            anyConstructed<AuthTokenRepositoryImpl>().setToken(any())
        }

        verify(exactly = 1) {
            Logger.w(Exponea, any())
        }

        assertTrue(logMessageSlot.isCaptured)
        assertEquals(
            "Current integration does not support SDK auth token operations, set operation will be ignored.",
            logMessageSlot.captured
        )
    }
}
