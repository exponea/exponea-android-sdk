package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.PushTokenRepository
import com.exponea.sdk.services.IntegrationConfigFactory
import com.exponea.sdk.util.TokenType
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PushNotificationSelfCheckManagerImplTest {

    private lateinit var context: Context
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var tokenRepository: PushTokenRepository
    private lateinit var flushManager: FlushManager
    private lateinit var exponeaService: ExponeaService
    private lateinit var integrationConfigFactory: IntegrationConfigFactory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds().apply {
            this.cookie = "cookie-123"
            this.externalIds = hashMapOf()
        }
        tokenRepository = mockk()
        every { tokenRepository.get() } returns "push-token-123"
        every { tokenRepository.getLastTokenType() } returns TokenType.FCM
        flushManager = mockk()
        every { flushManager.isRunning } returns false
        every { flushManager.flushData(any()) } answers {
            firstArg<((Result<Unit>) -> Unit)?>()?.invoke(Result.success(Unit))
        }
        exponeaService = mockk()
        integrationConfigFactory = IntegrationConfigFactory(
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(
                    baseUrl = "https://base-url.com",
                    projectToken = "mock-project-token",
                    authorization = "Token auth"
                )
            )
        )
    }

    @After
    fun after() {
        unmockkAll()
    }

    @Test
    fun `forwards configured applicationId to self-check endpoint`() {
        stubPostPushSelfCheckAsFailure()
        val manager = buildManager(applicationId = "abc-app")

        runBlocking { manager.startInternal() }

        verify(exactly = 1) {
            exponeaService.postPushSelfCheck(
                any<ProjectConfig>(),
                any<CustomerIds>(),
                any<String>(),
                any<TokenType>(),
                "abc-app"
            )
        }
    }

    private fun buildManager(applicationId: String): PushNotificationSelfCheckManagerImpl {
        return PushNotificationSelfCheckManagerImpl(
            context,
            customerIdsRepository,
            tokenRepository,
            flushManager,
            exponeaService,
            integrationConfigFactory,
            applicationId,
            operationsTimeout = 100L
        )
    }

    private fun stubPostPushSelfCheckAsFailure() {
        val call = mockk<Call>()
        // Resume the suspendCoroutine in requestSelfCheckPush via onFailure so that
        // startInternal terminates promptly without waiting for a real push.
        every { call.enqueue(any()) } answers {
            val callback = it.invocation.args[0] as Callback
            callback.onFailure(call, IOException("test-forced-failure"))
        }
        every {
            exponeaService.postPushSelfCheck(
                any<ProjectConfig>(),
                any<CustomerIds>(),
                any<String>(),
                any<TokenType>(),
                any<String>()
            )
        } returns call
    }
}
