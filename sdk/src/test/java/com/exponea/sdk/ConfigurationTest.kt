package com.exponea.sdk

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.HttpLoggingLevel.BASIC
import com.exponea.sdk.models.ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConfigurationTest : ExponeaSDKTest() {

    private fun setupExponea(
        authorization: String,
        projectToken: String = "projectToken",
        projectMapping: Map<EventType, List<ExponeaProject>>? = null
    ) {
        val configuration = ExponeaConfiguration(
            projectToken = projectToken,
            authorization = authorization
        )
        projectMapping?.let {
            configuration.projectRouteMap = it
        }
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), configuration)
    }

    @Rule @JvmField
    val expectedException = ExpectedException.none()

    @Test
    fun `should initialize with correct authorization`() {
        setupExponea("Token asdf")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should throw error when initializing sdk with basic authorization`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("""
            Basic authentication is not supported by mobile SDK for security reasons.
        """.trimIndent())
        setupExponea("Basic asdf")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should throw error when initializing sdk with unknown authorization`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("Use 'Token <access token>' as authorization for SDK.")
        setupExponea("asdf")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should initialize SDK from file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        @Suppress("DEPRECATION")
        Exponea.initFromFile(context)
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should initialize with correct secured authorization`() {
        setupExponea("Token asdf")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should use basic token for no secured token`() {
        val basicToken = "Token asdf"
        setupExponea(basicToken)
        assertEquals(
            basicToken,
            Exponea.componentForTesting.projectFactory.mutualExponeaProject.authorization
        )
    }

    @Test
    fun `should deserialize empty config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration()
        ExponeaConfigRepository.set(context, runTimeConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(runTimeConfig, storedConfig)
    }

    @Test
    fun `should deserialize basic config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com"
        )
        ExponeaConfigRepository.set(context, runTimeConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(runTimeConfig, storedConfig)
    }

    @Test
    fun `should deserialize full config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration(
            projectToken = "project-token",
            projectRouteMap = mapOf(EventType.TRACK_CUSTOMER to listOf(ExponeaProject(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            ))),
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com",
            httpLoggingLevel = BASIC,
            maxTries = 20,
            sessionTimeout = 20.0,
            campaignTTL = 20.0,
            automaticSessionTracking = true,
            automaticPushNotification = true,
            pushIcon = 1,
            pushAccentColor = 1,
            pushChannelName = "Push",
            pushChannelDescription = "Description",
            pushChannelId = "1",
            pushNotificationImportance = NotificationManager.IMPORTANCE_HIGH,
            defaultProperties = hashMapOf("def" to "val"),
            tokenTrackFrequency = EVERY_LAUNCH,
            allowDefaultCustomerProperties = true
        )
        ExponeaConfigRepository.set(context, runTimeConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(runTimeConfig, storedConfig)
    }

    @Test
    fun `should initialize with correct project token`() {
        setupExponea("Token asdf", "abcd-1234-fgh")
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should initialize with correct project mapping`() {
        setupExponea(
            "Token asdf",
            "abcd-1234-fgh",
            mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "project-token",
                        authorization = "Token mock-auth"
                    )
                )
            )
        )
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun `should throw error when initializing sdk with empty project token`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("""
            Project token provided is not valid. Project token cannot be empty string.
        """.trimIndent())
        setupExponea("Basic asdf", "")
        assertEquals(Exponea.isInitialized, false)
    }

    @Test
    fun `should throw error when initializing sdk with empty project token in mapping`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("""
            Project token provided is not valid. Project token cannot be empty string.
        """.trimIndent())
        setupExponea(
            "Token asdf",
            "abcd-1234-fgh",
            mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "",
                        authorization = "Token mock-auth"
                    )
                )
            )
        )
        assertEquals(Exponea.isInitialized, false)
    }

    @Test
    fun `should throw error when initializing sdk with invalid project token`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("""
            Project token provided is not valid. Only alphanumeric symbols and dashes are allowed in project token.
        """.trimIndent())
        setupExponea("Basic asdf", "invalid_token_value")
        assertEquals(Exponea.isInitialized, false)
    }

    @Test
    fun `should throw error when initializing sdk with invalid project token in mapping`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage("""
            Project mapping for event type TRACK_CUSTOMER is not valid. Project token provided is not valid. Only alphanumeric symbols and dashes are allowed in project token.
        """.trimIndent())
        setupExponea(
            "Token asdf",
            "abcd-1234-fgh",
            mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "invalid_token_value",
                        authorization = "Token mock-auth"
                    )
                )
            )
        )
        assertEquals(Exponea.isInitialized, false)
    }
}
