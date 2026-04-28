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
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.ExponeaConfigRepository.PREF_CONFIG
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.getId
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConfigurationTest : ExponeaSDKTest() {

    private fun setupExponea(
        authorization: String,
        integrationMapping: Map<EventType, List<ProjectConfig>>? = null,
        applicationId: String = Constants.ApplicationId.APP_ID_DEFAULT_VALUE
    ) = setupExponea(
        integrationConfig = ProjectConfig(projectToken = UUID.randomUUID().toString(), authorization = authorization),
        integrationMapping = integrationMapping,
        applicationId = applicationId
    )

    private fun setupExponea(
        integrationConfig: IntegrationConfig,
        integrationMapping: Map<EventType, List<ProjectConfig>>? = null,
        applicationId: String = Constants.ApplicationId.APP_ID_DEFAULT_VALUE
    ) {
        val configuration = ExponeaConfiguration(
            integrationConfig = integrationConfig,
            applicationId = applicationId
        )
        integrationMapping?.let {
            configuration.integrationRouteMap = it
        }
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), configuration)
    }

    @Test
    fun `should initialize with correct authorization`() {
        setupExponea("Token asdf")
        assertEquals(true, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with basic authorization`() {
        val exception = assertThrows(InvalidConfigurationException::class.java) {
            setupExponea("Basic asdf")
        }
        assertThat(
            exception.message,
            equalTo(
                """
                Basic authentication is not supported by mobile SDK for security reasons.
                Use Token authentication instead.
                For more details see https://documentation.bloomreach.com/engagement/reference/technical-information#public-api-access
                """.trimIndent()
            )
        )
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with unknown authorization`() {
        assertThrows(
            "Use 'Token <access token>' as authorization for SDK.",
            InvalidConfigurationException::class.java
        ) {
            setupExponea("asdf")
        }
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should initialize SDK from file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        @Suppress("DEPRECATION")
        Exponea.initFromFile(context)
        assertEquals(true, Exponea.isInitialized)
    }

    @Test
    fun `should initialize with correct secured authorization`() {
        setupExponea("Token asdf")
        assertEquals(true, Exponea.isInitialized)
    }

    @Test
    fun `should deserialize empty config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration()
        ExponeaConfigRepository.set(context, runTimeConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(runTimeConfig, storedConfig)
    }

    @Test
    fun `should serialize and deserialize basic deprecated config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration(
            projectToken = "project-token",
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com"
        )
        ExponeaConfigRepository.set(context, runTimeConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(runTimeConfig, storedConfig)
    }

    @Test
    fun `should serialize and deserialize full deprecated config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runTimeConfig = ExponeaConfiguration(
            projectToken = "project-token",
            projectRouteMap = mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "project-token",
                        authorization = "Token mock-auth"
                    )
                )
            ),
            authorization = "Token mock-auth",
            baseURL = "https://api.exponea.com",
            httpLoggingLevel = BASIC,
            maxTries = 20,
            sessionTimeout = 60.0,
            campaignTTL = 20.0,
            automaticSessionTracking = true,
            automaticPushNotification = true,
            requirePushAuthorization = false,
            pushIcon = 1,
            pushAccentColor = 1,
            pushChannelName = "Push",
            pushChannelDescription = "Description",
            pushChannelId = "1",
            pushNotificationImportance = NotificationManager.IMPORTANCE_HIGH,
            defaultProperties = hashMapOf("def" to "val"),
            tokenTrackFrequency = EVERY_LAUNCH,
            allowDefaultCustomerProperties = false,
            advancedAuthEnabled = false,
            inAppContentBlockPlaceholdersAutoLoad = listOf("placeholder_1", "placeholder_2"),
            appInboxDetailImageInset = 1,
            allowWebViewCookies = true,
            manualSessionAutoClose = false,
            applicationId = "custom-app-id"
        )
        ExponeaConfigRepository.set(context, runTimeConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(runTimeConfig, storedConfig)
    }

    @Test
    fun `should deserialize full deprecated config`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val expectedResult = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                projectToken = "project-token",
                baseUrl = "https://api.exponea.com",
                authorization = "Token mock-auth"
            ),
            integrationRouteMap = mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ProjectConfig(
                        projectToken = "project-token",
                        baseUrl = "https://api.exponea.com",
                        authorization = "Token mock-auth"
                    )
                )
            ),
            httpLoggingLevel = BASIC,
            maxTries = 20,
            sessionTimeout = 60.0,
            campaignTTL = 20.0,
            automaticSessionTracking = true,
            automaticPushNotification = true,
            requirePushAuthorization = false,
            pushIcon = 1,
            pushAccentColor = 1,
            pushChannelName = "Push",
            pushChannelDescription = "Description",
            pushChannelId = "1",
            pushNotificationImportance = NotificationManager.IMPORTANCE_HIGH,
            defaultProperties = hashMapOf("def" to "val"),
            tokenTrackFrequency = EVERY_LAUNCH,
            allowDefaultCustomerProperties = false,
            advancedAuthEnabled = true,
            inAppContentBlockPlaceholdersAutoLoad = listOf("placeholder_1", "placeholder_2"),
            appInboxDetailImageInset = 1,
            allowWebViewCookies = true,
            manualSessionAutoClose = false,
            applicationId = "custom-app-id"
        )
        expectedResult.baseURL = "https://api.exponea.com"
        expectedResult.projectToken = "project-token"
        expectedResult.authorization = "Token mock-auth"
        expectedResult.projectRouteMap = mapOf(
            EventType.TRACK_CUSTOMER to listOf(
                ExponeaProject(
                    projectToken = "project-token",
                    baseUrl = "https://api.exponea.com",
                    authorization = "Token mock-auth"
                )
            )
        )

        val serializedConfig = """
            {
              "projectToken" : "project-token",
              "projectRouteMap" : {
                "TRACK_CUSTOMER" : [ {
                  "baseUrl" : "https://api.exponea.com",
                  "projectToken" : "project-token",
                  "authorization" : "Token mock-auth",
                  "inAppContentBlockPlaceholdersAutoLoad" : [ ]
                } ]
              },
              "authorization" : "Token mock-auth",
              "baseURL" : "https://api.exponea.com",
              "httpLoggingLevel" : "BASIC",
              "maxTries" : 20,
              "sessionTimeout" : 60.0,
              "campaignTTL" : 20.0,
              "automaticSessionTracking" : true,
              "automaticPushNotification" : true,
              "requirePushAuthorization" : false,
              "pushIcon" : 1,
              "pushAccentColor" : 1,
              "pushChannelName" : "Push",
              "pushChannelDescription" : "Description",
              "pushChannelId" : "1",
              "pushNotificationImportance" : 4,
              "defaultProperties" : {
                "def" : "val"
              },
              "tokenTrackFrequency" : "EVERY_LAUNCH",
              "allowDefaultCustomerProperties" : false,
              "advancedAuthEnabled" : true,
              "inAppContentBlockPlaceholdersAutoLoad" : [ "placeholder_1", "placeholder_2" ],
              "appInboxDetailImageInset" : 1,
              "allowWebViewCookies" : true,
              "manualSessionAutoClose" : false,
              "applicationId" : "custom-app-id"
            }
        """.trimIndent()
        ExponeaPreferencesImpl(context).setString(PREF_CONFIG, serializedConfig)
        val deserializedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(deserializedConfig)
        assertDeepEquals(expectedResult, deserializedConfig)
    }

    @Test
    fun `should serialize and deserialize basic config with project integration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                projectToken = "project-token",
                authorization = "Token mock-auth",
                baseUrl = "https://api.exponea.com"
            )
        )
        ExponeaConfigRepository.set(context, configuration)
        val deserializedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(deserializedConfig)
        assertDeepEquals(configuration, deserializedConfig)
    }

    @Test
    fun `should serialize and deserialize basic config with data hub integration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            integrationConfig = StreamConfig(
                baseUrl = "https://api.exponea.com",
                streamId = "stream-id"
            )
        )
        ExponeaConfigRepository.set(context, configuration)
        val deserializedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(deserializedConfig)
        assertDeepEquals(configuration, deserializedConfig)
    }

    @Test
    fun `should serialize and deserialize full config with project integration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            ),
            integrationRouteMap = mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ProjectConfig(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "project-token",
                        authorization = "Token mock-auth"
                    )
                )
            ),
            httpLoggingLevel = BASIC,
            maxTries = 20,
            sessionTimeout = 60.0,
            campaignTTL = 20.0,
            automaticSessionTracking = true,
            automaticPushNotification = true,
            requirePushAuthorization = false,
            pushIcon = 1,
            pushAccentColor = 1,
            pushChannelName = "Push",
            pushChannelDescription = "Description",
            pushChannelId = "1",
            pushNotificationImportance = NotificationManager.IMPORTANCE_HIGH,
            defaultProperties = hashMapOf("def" to "val"),
            tokenTrackFrequency = EVERY_LAUNCH,
            allowDefaultCustomerProperties = false,
            advancedAuthEnabled = false,
            inAppContentBlockPlaceholdersAutoLoad = listOf("placeholder_1", "placeholder_2"),
            appInboxDetailImageInset = 1,
            allowWebViewCookies = true,
            manualSessionAutoClose = false,
            applicationId = "custom-app-id"
        )
        ExponeaConfigRepository.set(context, configuration)
        val deserializedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(deserializedConfig)
        assertDeepEquals(configuration, deserializedConfig)
    }

    @Test
    fun `should serialize and deserialize full config with data hub integration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            integrationConfig = StreamConfig(
                baseUrl = "https://api.exponea.com",
                streamId = "stream-id"
            ),
            integrationRouteMap = mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ProjectConfig(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "project-token",
                        authorization = "Token mock-auth"
                    )
                )
            ),
            httpLoggingLevel = BASIC,
            maxTries = 20,
            sessionTimeout = 60.0,
            campaignTTL = 20.0,
            automaticSessionTracking = true,
            automaticPushNotification = true,
            requirePushAuthorization = false,
            pushIcon = 1,
            pushAccentColor = 1,
            pushChannelName = "Push",
            pushChannelDescription = "Description",
            pushChannelId = "1",
            pushNotificationImportance = NotificationManager.IMPORTANCE_HIGH,
            defaultProperties = hashMapOf("def" to "val"),
            tokenTrackFrequency = EVERY_LAUNCH,
            allowDefaultCustomerProperties = false,
            advancedAuthEnabled = false,
            inAppContentBlockPlaceholdersAutoLoad = listOf("placeholder_1", "placeholder_2"),
            appInboxDetailImageInset = 1,
            allowWebViewCookies = true,
            manualSessionAutoClose = false,
            applicationId = "custom-app-id"
        )
        ExponeaConfigRepository.set(context, configuration)
        val deserializedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(deserializedConfig)
        assertDeepEquals(configuration, deserializedConfig)
    }

    @Test
    fun `should deserialize config after switch project`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            )
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(sdkConfig, storedConfig)
        // switch project
        val baseUrlSwitch = "https://switch-api.exponea.com"
        val projectTokenSwitch = "switch-project-token"
        val authorizationSwitch = "Token switch-mock-auth"
        Exponea.anonymize(
            integrationConfig = ProjectConfig(
                baseUrl = baseUrlSwitch,
                projectToken = projectTokenSwitch,
                authorization = authorizationSwitch
            )
        )
        val storedConfigAfterSwitch = ExponeaConfigRepository.get(context)
        assertEquals(baseUrlSwitch, storedConfigAfterSwitch?.integrationConfig?.baseUrl)
        assertEquals(projectTokenSwitch, storedConfigAfterSwitch?.integrationConfig?.getId())
        assertEquals(
            authorizationSwitch,
            (storedConfigAfterSwitch?.integrationConfig as? ProjectConfig)?.authorization
        )
    }

    @Test
    fun `should initialize with correct project token`() {
        setupExponea(integrationConfig = ProjectConfig(projectToken = "abcd-1234-fgh", authorization = "Token asdf"))
        assertEquals(true, Exponea.isInitialized)
    }

    @Test
    fun `should initialize with correct stream id`() {
        setupExponea(integrationConfig = StreamConfig(streamId = "abcd-1234-fgh"))
        assertEquals(true, Exponea.isInitialized)
    }

    @Test
    fun `should initialize with correct project mapping`() {
        setupExponea(
            ProjectConfig(projectToken = "abcd-1234-fgh", authorization = "Token asdf"),
            mapOf(
                EventType.TRACK_CUSTOMER to listOf(
                    ProjectConfig(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "project-token",
                        authorization = "Token mock-auth"
                    )
                )
            )
        )
        assertEquals(true, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with empty project token`() {
        assertThrows(
            "Provided project token is not valid. Cannot be empty string.",
            InvalidConfigurationException::class.java
        ) {
            setupExponea(ProjectConfig(projectToken = "", authorization = "Basic asdf"))
        }
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with empty stream id`() {
        assertThrows(
            "Provided stream id is not valid. Cannot be empty string.",
            InvalidConfigurationException::class.java
        ) {
            setupExponea(StreamConfig(streamId = ""))
        }
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with empty project token in mapping`() {
        assertThrows(
            "Provided project token is not valid. Cannot be empty string.",
            InvalidConfigurationException::class.java
        ) {
            setupExponea(
                ProjectConfig(projectToken = "abcd-1234-fgh", authorization = "Token asdf"),
                mapOf(
                    EventType.TRACK_CUSTOMER to listOf(
                        ProjectConfig(
                            baseUrl = "https://api.exponea.com",
                            projectToken = "",
                            authorization = "Token mock-auth"
                        )
                    )
                )
            )
        }
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with invalid project token`() {
        assertThrows(
            "Provided project token is not valid. Only alphanumeric symbols and dashes are allowed.",
            InvalidConfigurationException::class.java
        ) {
            setupExponea(ProjectConfig(projectToken = "invalid_token_value", authorization = "Basic asdf"))
        }
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with invalid stream id`() {
        assertThrows(
            "Provided stream id is not valid. Only alphanumeric symbols and dashes are allowed.",
            InvalidConfigurationException::class.java
        ) {
            setupExponea(StreamConfig(streamId = "invalid_stream_id"))
        }
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should throw error when initializing sdk with invalid project token in mapping`() {
        assertThrows(
            """
                Integration route mapping for event type TRACK_CUSTOMER is not valid.
                Provided project token is not valid. Only alphanumeric symbols and dashes are allowed.
                """".trimIndent(),
            InvalidConfigurationException::class.java
        ) {
            setupExponea(
                ProjectConfig(projectToken = "abcd-1234-fgh", authorization = "Token asdf"),
                mapOf(
                    EventType.TRACK_CUSTOMER to listOf(
                        ProjectConfig(
                            baseUrl = "https://api.exponea.com",
                            projectToken = "invalid_token_value",
                            authorization = "Token mock-auth"
                        )
                    )
                )
            )
        }
        assertEquals(false, Exponea.isInitialized)
    }

    @Test
    fun `should update sessionTimeout conf in local storage`() {
        // de-init to be able init SDK
        resetExponea()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkConfig = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            ),
            sessionTimeout = 10.0
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(sdkConfig, storedConfig)
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
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            ),
            automaticSessionTracking = false
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(sdkConfig, storedConfig)
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
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            ),
            automaticPushNotification = false
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(sdkConfig, storedConfig)
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
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            ),
            campaignTTL = 10.0
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(sdkConfig, storedConfig)
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
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            ),
            defaultProperties = hashMapOf("defTestProp" to "defTestVal")
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, sdkConfig)
        val storedConfig = ExponeaConfigRepository.get(context)
        assertNotNull(storedConfig)
        assertDeepEquals(sdkConfig, storedConfig)
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
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            )
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
            integrationConfig = ProjectConfig(
                baseUrl = "https://api.exponea.com",
                projectToken = "project-token",
                authorization = "Token mock-auth"
            )
        )
        Exponea.isStopped = true
        ExponeaConfigRepository.set(context, runTimeConfig)
        assertEquals("none", ExponeaPreferencesImpl(context).getString(PREF_CONFIG, "none"))
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
            assertEquals(true, Exponea.isInitialized)
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
            assertThrows(
                "The provided applicationId is not in the correct format.",
                InvalidConfigurationException::class.java
            ) {
                setupExponea(authorization = "Token asdf", applicationId = appId)
            }

            assertEquals(false, Exponea.isInitialized)
            resetExponea()
        }
    }

    @Test
    fun `check invalid extra long applicationId format`() {
        assertThrows(
            "The provided applicationId exceeds the maximum length of $APP_ID_MAX_LENGTH characters.",
            InvalidConfigurationException::class.java
        ) {
            setupExponea(
                authorization = "Token asdf",
                applicationId = "012345678901234567890123456789012345678901234567890" // more than 50 characters
            )
        }
        assertEquals(false, Exponea.isInitialized)
    }

    private fun assertDeepEquals(expected: ExponeaConfiguration, actual: ExponeaConfiguration) {
        assertThat(actual.integrationConfig::class, equalTo(expected.integrationConfig::class))
        assertThat(actual.integrationConfig.baseUrl, equalTo(expected.integrationConfig.baseUrl))
        assertThat(actual.integrationConfig.getId(), equalTo(expected.integrationConfig.getId()))
        if (actual.integrationConfig is ProjectConfig && expected.integrationConfig is ProjectConfig) {
            assertThat(
                (actual.integrationConfig as ProjectConfig).authorization,
                equalTo((expected.integrationConfig as ProjectConfig).authorization)
            )
        }

        assertThat(actual.integrationRouteMap.size, equalTo(expected.integrationRouteMap.size))
        expected.integrationRouteMap.forEach { (eventType, expectedList) ->
            val actualList = actual.integrationRouteMap[eventType]
            assertThat(actualList?.size, equalTo(expectedList.size))
            expectedList.zip(actualList ?: emptyList()).forEach { (exp, act) ->
                assertThat(act::class, equalTo(exp::class))
                assertThat(act.baseUrl, equalTo(exp.baseUrl))
                assertThat(act.authorization, equalTo(exp.authorization))
                assertThat(act.getId(), equalTo(exp.getId()))
            }
        }

        // Custom comparison for projectRouteMap
        assertThat(actual.projectRouteMap.size, equalTo(expected.projectRouteMap.size))
        expected.projectRouteMap.forEach { (eventType, expectedList) ->
            val actualList = actual.projectRouteMap[eventType]
            assertThat(actualList?.size, equalTo(expectedList.size))
            expectedList.zip(actualList ?: emptyList()).forEach { (exp, act) ->
                assertThat(act.baseUrl, equalTo(exp.baseUrl))
                assertThat(act.projectToken, equalTo(exp.projectToken))
                assertThat(act.authorization, equalTo(exp.authorization))
            }
        }

        assertThat(actual.projectToken, equalTo(expected.projectToken))
        assertThat(actual.baseURL, equalTo(expected.baseURL))
        assertThat(actual.authorization, equalTo(expected.authorization))
        assertThat(actual.httpLoggingLevel, equalTo(expected.httpLoggingLevel))
        assertThat(actual.maxTries, equalTo(expected.maxTries))
        assertThat(actual.sessionTimeout, equalTo(expected.sessionTimeout))
        assertThat(actual.campaignTTL, equalTo(expected.campaignTTL))
        assertThat(actual.automaticSessionTracking, equalTo(expected.automaticSessionTracking))
        assertThat(actual.automaticPushNotification, equalTo(expected.automaticPushNotification))
        assertThat(actual.requirePushAuthorization, equalTo(expected.requirePushAuthorization))
        assertThat(actual.pushIcon, equalTo(expected.pushIcon))
        assertThat(actual.pushAccentColor, equalTo(expected.pushAccentColor))
        assertThat(actual.pushChannelName, equalTo(expected.pushChannelName))
        assertThat(actual.pushChannelDescription, equalTo(expected.pushChannelDescription))
        assertThat(actual.pushChannelId, equalTo(expected.pushChannelId))
        assertThat(actual.pushNotificationImportance, equalTo(expected.pushNotificationImportance))
        assertThat(actual.defaultProperties, equalTo(expected.defaultProperties))
        assertThat(actual.tokenTrackFrequency, equalTo(expected.tokenTrackFrequency))
        assertThat(actual.allowDefaultCustomerProperties, equalTo(expected.allowDefaultCustomerProperties))
        assertThat(actual.advancedAuthEnabled, equalTo(expected.advancedAuthEnabled))
        assertThat(
            actual.inAppContentBlockPlaceholdersAutoLoad,
            equalTo(expected.inAppContentBlockPlaceholdersAutoLoad)
        )
        assertThat(actual.appInboxDetailImageInset, equalTo(expected.appInboxDetailImageInset))
        assertThat(actual.allowWebViewCookies, equalTo(expected.allowWebViewCookies))
        assertThat(actual.manualSessionAutoClose, equalTo(expected.manualSessionAutoClose))
        assertThat(actual.applicationId, equalTo(expected.applicationId))
    }

    @Test
    fun `should not warn when using ProjectConfig with advancedAuthEnabled`() {
        mockkObject(Logger)
        every { Logger.w(any(), any()) } returns Unit
        val config = ExponeaConfiguration(
            integrationConfig = ProjectConfig(projectToken = "mock-token", authorization = "Token mock-auth"),
            advancedAuthEnabled = true
        )

        config.validate()

        assertEquals(true, config.advancedAuthEnabled)
        verify(exactly = 0) {
            Logger.w(config, any())
        }
    }

    @Test
    fun `should not warn when using ProjectConfig with integrationRouteMap`() {
        mockkObject(Logger)
        every { Logger.w(any(), any()) } returns Unit
        val config = ExponeaConfiguration(
            integrationConfig = ProjectConfig(projectToken = "mock-token", authorization = "Token mock-auth"),
            integrationRouteMap = hashMapOf(
                EventType.INSTALL to arrayListOf(
                    ProjectConfig(
                        projectToken = "mock-token-2",
                        authorization = "Token mock-auth-2"
                    )
                )
            )
        )

        config.validate()

        assertEquals(1, config.integrationRouteMap.size)
        verify(exactly = 0) {
            Logger.w(
                config,
                any()
            )
        }
    }

    @Test
    fun `should validate StreamConfig with advancedAuthEnabled - logs warning but does not throw`() {
        mockkObject(Logger)
        every { Logger.w(any(), any()) } returns Unit
        val config = ExponeaConfiguration(
            integrationConfig = StreamConfig(streamId = "mock-stream-id"),
            advancedAuthEnabled = true
        )

        config.validate()

        assertEquals(true, config.advancedAuthEnabled)
        verify(exactly = 1) {
            Logger.w(
                config,
                "Advanced authentication is only supported when using ProjectConfig. " +
                        "This setting will be ignored for StreamConfig."
            )
        }
    }

    @Test
    fun `should validate StreamConfig with integrationRouteMap - logs warning but does not throw`() {
        mockkObject(Logger)
        every { Logger.w(any(), any()) } returns Unit
        val config = ExponeaConfiguration(
            integrationConfig = StreamConfig(streamId = "mock-stream-id"),
            integrationRouteMap = hashMapOf(
                EventType.INSTALL to arrayListOf(
                    ProjectConfig(
                        projectToken = "mock-token-2",
                        authorization = "Token mock-auth-2"
                    )
                )
            )
        )

        config.validate()

        assertEquals(1, config.integrationRouteMap.size)
        verify(exactly = 1) {
            Logger.w(
                config,
                "Integration route mapping is only supported when using ProjectConfig. " +
                        "This setting will be ignored for StreamConfig."
            )
        }
    }

    @Test
    fun `should throw when baseUrl is empty baseUrl`() {
        val config = ExponeaConfiguration(
            integrationConfig = StreamConfig(baseUrl = "", streamId = "mock-stream-id")
        )
        val exception = assertThrows(InvalidConfigurationException::class.java) {
            config.validate()
        }
        assertThat(exception.message, equalTo("Provided baseUrl is not valid. Cannot be empty string."))
    }

    @Test
    fun `should throw when baseUrl is missing http or https scheme`() {
        val config = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                baseUrl = "api.exponea.com",
                projectToken = "mock-token",
                authorization = "Token mock-auth"
            )
        )
        val exception = assertThrows(InvalidConfigurationException::class.java) {
            config.validate()
        }
        assertThat(
            exception.message,
            equalTo("Provided baseUrl is not valid. Must start with 'http://' or 'https://' (got 'api.exponea.com').")
        )
    }

    @Test
    fun `should throw when integrationRouteMap entry has invalid baseUrl`() {
        val eventType = EventType.INSTALL
        val invalidUrl = "not-a-valid-url"

        val config = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                projectToken = "mock-token",
                authorization = "Token mock-auth"
            ),
            integrationRouteMap = hashMapOf(
                eventType to listOf(
                    ProjectConfig(
                        baseUrl = invalidUrl,
                        projectToken = "mock-token-2",
                        authorization = "Token mock-auth-2"
                    )
                )
            )
        )
        val exception = assertThrows(InvalidConfigurationException::class.java) {
            config.validate()
        }
        assertThat(
            exception.message,
            equalTo("Integration route mapping for event type $eventType is not valid. " +
                    "Provided baseUrl is not valid. Must start with 'http://' or 'https://' (got '$invalidUrl').")
        )
    }

    @Test
    fun `should throw when deprecated constructor receives empty baseURL`() {
        @Suppress("DEPRECATION")
        val config = ExponeaConfiguration(
            projectToken = "mock-token",
            authorization = "Token mock-auth",
            baseURL = ""
        )
        val exception = assertThrows(InvalidConfigurationException::class.java) {
            config.validate()
        }
        assertThat(exception.message, equalTo("Provided baseUrl is not valid. Cannot be empty string."))
    }
}
