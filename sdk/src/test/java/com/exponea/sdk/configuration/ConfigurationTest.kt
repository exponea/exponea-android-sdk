package com.exponea.sdk.configuration

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.Constants.ApplicationId.APP_ID_MAX_LENGTH
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.HttpLoggingLevel.BASIC
import com.exponea.sdk.models.ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
        projectMapping: Map<EventType, List<ExponeaProject>>? = null,
        applicationId: String = Constants.ApplicationId.APP_ID_DEFAULT_VALUE
    ) {
        val configuration = ExponeaConfiguration(
            projectToken = projectToken,
            authorization = authorization,
            applicationId = applicationId
        )
        projectMapping?.let {
            configuration.projectRouteMap = it
        }
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), configuration)
    }

    @Rule
    @JvmField
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
            sessionTimeout = 60.0,
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
    fun `should deserialize config after switch project`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com"
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(sdkConfig, storedConfig)
        // switch project
        val baseUrlSwitch = "https://switch-api.exponea.com"
        val projectTokenSwitch = "switch-project-token"
        val authorizationSwitch = "Token switch-mock-auth"
        Exponea.anonymize(
            exponeaProject = ExponeaProject(
                baseUrl = baseUrlSwitch,
                projectToken = projectTokenSwitch,
                authorization = authorizationSwitch
            )
        )
        val storedConfigAfterSwitch = ExponeaConfigRepository.get(context)
        assertEquals(baseUrlSwitch, storedConfigAfterSwitch?.baseURL)
        assertEquals(projectTokenSwitch, storedConfigAfterSwitch?.projectToken)
        assertEquals(authorizationSwitch, storedConfigAfterSwitch?.authorization)
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

    @Test
    fun `should update sessionTimeout conf in local storage`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com",
            sessionTimeout = 10.0
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(sdkConfig, storedConfig)
        assertNotNull(storedConfig)
        assertEquals(10.0, storedConfig.sessionTimeout)
        // update sessionTimeout via API
        Exponea.sessionTimeout = 20.0
        val storedConfigAfterUpdate = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfigAfterUpdate)
        assertEquals(20.0, storedConfigAfterUpdate.sessionTimeout)
    }

    @Test
    fun `should update automaticSessionTracking conf in local storage`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com",
            automaticSessionTracking = false
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(sdkConfig, storedConfig)
        assertNotNull(storedConfig)
        assertFalse(storedConfig.automaticSessionTracking)
        // update automaticSessionTracking via API
        Exponea.isAutomaticSessionTracking = true
        val storedConfigAfterUpdate = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfigAfterUpdate)
        assertTrue(storedConfigAfterUpdate.automaticSessionTracking)
    }

    @Test
    fun `should update automaticPushNotification conf in local storage`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com",
            automaticPushNotification = false
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(sdkConfig, storedConfig)
        assertNotNull(storedConfig)
        assertFalse(storedConfig.automaticPushNotification)
        // update automaticPushNotification via API
        Exponea.isAutoPushNotification = true
        val storedConfigAfterUpdate = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfigAfterUpdate)
        assertTrue(storedConfigAfterUpdate.automaticPushNotification)
    }

    @Test
    fun `should update campaignTTL conf in local storage`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com",
            campaignTTL = 10.0
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(sdkConfig, storedConfig)
        assertNotNull(storedConfig)
        assertEquals(10.0, storedConfig.campaignTTL)
        // update campaignTTL via API
        Exponea.campaignTTL = 20.0
        val storedConfigAfterUpdate = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfigAfterUpdate)
        assertEquals(20.0, storedConfigAfterUpdate.campaignTTL)
    }

    @Test
    fun `should update defaultProperties conf in local storage`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com",
            defaultProperties = hashMapOf("defTestProp" to "defTestVal")
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(sdkConfig, storedConfig)
        assertNotNull(storedConfig)
        assertEquals("defTestVal", storedConfig.defaultProperties["defTestProp"])
        // update defaultProperties via API
        Exponea.defaultProperties = hashMapOf("defTestPropUpdate" to "defTestValUpdate")
        val storedConfigAfterUpdate = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfigAfterUpdate)
        assertTrue(storedConfigAfterUpdate.automaticPushNotification)
        assertNull(storedConfigAfterUpdate.defaultProperties["defTestProp"])
        assertEquals("defTestValUpdate", storedConfigAfterUpdate.defaultProperties["defTestPropUpdate"])
    }

    @Test
    fun `should not load config if SDK is stopped`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com"
        )
        ExponeaConfigRepository.set(context, runTimeConfig)
        Exponea.isStopped = true
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNull(storedConfig)
    }

    @Test
    fun `should not store config if SDK is stopped`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com"
        )
        Exponea.isStopped = true
        ExponeaConfigRepository.set(context, runTimeConfig)
        assertEquals("none", ExponeaPreferencesImpl(context).getString(ExponeaConfigRepository.PREF_CONFIG, "none"))
    }

    @Test
    fun `should deserialize config with custom applicationId`() {
        val customAppId = "custom-app-id"
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration(applicationId = customAppId)
        ExponeaConfigRepository.set(context, runTimeConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertEquals(customAppId, storedConfig?.applicationId)
    }

    @Test
    fun `should initialize with all valid applicationIds`() {
        val validIds = listOf(
            "default-application",
            "default.application",
            "0.default.application",
            "0-default-application",
            "aplicationaplication",
            "187264817649187649",
            "aaaaaaaaa",
            "000000000",
            "01234567890123456789012345678901234567890123456789" // 50 characters

        )

        validIds.forEach { appId ->
            setupExponea(authorization = "Token asdf", applicationId = appId)
            assertEquals(Exponea.isInitialized, true)
            resetExponea()
        }
    }

    @Test
    fun `should fail to initialize with invalid applicationIds`() {
        val invalidIds = listOf(
            ".example-app.demo.sdk",
            "0.default.application.",
            "-com.example-app.demo.sdk",
            "Default-application",
            "Default.application",
            "default-Application",
            "com.example-app.demo.sdk!",
            "0..default-application",
            "0--default-application",
            "!@@#!@$$%#%$$%*$%*"
        )

        invalidIds.forEach { appId ->
            expectedException.expect(InvalidConfigurationException::class.java)
            expectedException.expectMessage("The provided applicationId is not in the correct format.")
            setupExponea(authorization = "Token asdf", applicationId = appId)
            assertEquals(Exponea.isInitialized, false)
            resetExponea()
        }
    }

    @Test
    fun `check invalid extra long applicationId format`() {
        expectedException.expect(InvalidConfigurationException::class.java)
        expectedException.expectMessage(
            "The provided applicationId exceeds the maximum length of $APP_ID_MAX_LENGTH characters."
        )
        setupExponea(
            authorization = "Token asdf",
            applicationId = "012345678901234567890123456789012345678901234567890") // more than 50 characters
        assertEquals(Exponea.isInitialized, false)
    }
}
