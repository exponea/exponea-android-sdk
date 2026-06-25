package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.DeviceIdManager
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.FlushFinishedCallback
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.manager.SegmentsManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.Constants.EventTypes.installation
import com.exponea.sdk.models.Constants.EventTypes.pushTokenTrack
import com.exponea.sdk.models.Constants.EventTypes.sessionStart
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfigurationOverrides
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.IntegrationConfiguration
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.SdkAuthCallback
import com.exponea.sdk.models.SdkAuthError
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.receiver.NotificationsPermissionReceiver
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AnonymizeTest : ExponeaSDKTest() {

    private fun expectedPushTokenProperties(
        context: Context,
        pushToken: String = "push_token",
        platform: String = "android",
        applicationId: String = "default-application",
        valid: Boolean,
        description: String,
        deviceId: String
    ): HashMap<String, Any> =
        DeviceProperties(context).toHashMap().apply {
            put("push_notification_token", pushToken)
            put("platform", platform)
            put("application_id", applicationId)
            put("valid", valid)
            put("description", description)
            put("device_id", deviceId)
        }

    private fun expectedTestEventProperties(deviceId: String): HashMap<String, Any> =
        hashMapOf(
            "name" to "test",
            "application_id" to "default-application",
            "device_id" to deviceId
        )

    private fun checkEvent(
        event: ExportedEvent,
        expectedEventType: String?,
        expectedProjectConfig: ProjectConfig,
        expectedUserId: String,
        expectedProperties: HashMap<String, Any>? = null
    ) {
        val transformedIntegrationConfiguration = IntegrationConfiguration(
            integrationId = expectedProjectConfig.projectToken,
            authorization = expectedProjectConfig.authorization,
            baseUrl = expectedProjectConfig.baseUrl,
            type = IntegrationConfigType.PROJECT
        )
        assertEquals(expectedEventType, event.type)
        assertEquals(transformedIntegrationConfiguration, event.integrationConfiguration)
        assertEquals(hashMapOf<String, String?>("cookie" to expectedUserId), event.customerIds)
        if (expectedProperties != null) assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `should anonymize sdk and switch projects`() = runInSingleThread { idleThreads ->
        mockkObject(NotificationsPermissionReceiver)
        every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns true
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialProjectConfig = ProjectConfig(
            "https://base-url.com",
            "project-token",
            "Token auth"
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(integrationConfig = initialProjectConfig))
        val testFirebaseToken = "push_token"
        val userId = Exponea.componentForTesting.customerIdsRepository.get().cookie

        Exponea.trackEvent(
            eventType = "test",
            properties = PropertiesList(hashMapOf("name" to "test")),
            timestamp = currentTimeSeconds()
        )
        Exponea.trackPushToken(testFirebaseToken)

        val newProjectConfig = ProjectConfig("https://other-base-url.com", "new_project_token", "Token other-auth")
        Exponea.anonymize(integrationConfig = newProjectConfig)
        Exponea.trackEvent(
            eventType = "test",
            properties = PropertiesList(hashMapOf("name" to "test")),
            timestamp = currentTimeSeconds()
        )
        idleThreads()
        val newUserId = Exponea.componentForTesting.customerIdsRepository.get().cookie
        val events = Exponea.componentForTesting.eventRepository.all()
        val deviceId = DeviceIdManager.getDeviceId(context)
        events.sortedBy { it.timestamp }
        assertEquals(9, events.size)
        checkEvent(events[0], installation, initialProjectConfig, userId!!, null)
        checkEvent(events[1], "test", initialProjectConfig, userId, expectedTestEventProperties(deviceId))
        checkEvent(
            events[2], pushTokenTrack, initialProjectConfig, userId, expectedPushTokenProperties(
                context = context,
                valid = true,
                description = Constants.PushPermissionStatus.PERMISSION_GRANTED,
                deviceId = deviceId
            )
        )
        checkEvent(events[3], Constants.EventTypes.sessionEnd, initialProjectConfig, userId, null)
        // anonymize is called. We clear push token in old user and track initial events for new user
        checkEvent(
            events[4], pushTokenTrack, initialProjectConfig, userId, expectedPushTokenProperties(
                context = context,
                valid = false,
                description = Constants.PushPermissionStatus.INVALIDATED_TOKEN,
                deviceId = deviceId
            )
        )
        checkEvent(events[5], installation, newProjectConfig, newUserId!!, null)
        checkEvent(events[6], sessionStart, newProjectConfig, newUserId, null)
        checkEvent(
            events[7], pushTokenTrack, newProjectConfig, newUserId, expectedPushTokenProperties(
                context = context,
                valid = true,
                description = Constants.PushPermissionStatus.PERMISSION_GRANTED,
                deviceId = deviceId
            )
        )
        checkEvent(events[8], "test", newProjectConfig, newUserId, expectedTestEventProperties(deviceId))
    }

    @Test
    fun `should not track session start on anonymize when automaticSessionTracking is off`() {
        runInSingleThread { idleThreads ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val initialProjectConfig = ProjectConfig(
                "https://base-url.com",
                "project-token",
                "Token auth"
            )
            val deviceId = DeviceIdManager.getDeviceId(context)
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(
                context,
                ExponeaConfiguration(integrationConfig = initialProjectConfig, automaticSessionTracking = false)
            )
            val userId = Exponea.componentForTesting.customerIdsRepository.get().cookie
            Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
            )
            val newProjectConfig = ProjectConfig("https://other-base-url.com", "new_project_token", "Token other-auth")
            Exponea.anonymize(integrationConfig = newProjectConfig)
            val newUserId = Exponea.componentForTesting.customerIdsRepository.get().cookie
            Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
            )
            idleThreads()
            val events = Exponea.componentForTesting.eventRepository.all()
            events.sortedBy { it.timestamp }
            assertEquals(expected = 4, actual = events.size)
            checkEvent(events[0], installation, initialProjectConfig, userId!!, null)
            checkEvent(events[1], "test", initialProjectConfig, userId, expectedTestEventProperties(deviceId))
            checkEvent(events[2], installation, newProjectConfig, newUserId!!, null)
            checkEvent(events[3], "test", newProjectConfig, newUserId, expectedTestEventProperties(deviceId))
        }
    }

    @Test
    fun `should track session start and end on anonymize when automaticSessionTracking is on`() {
        runInSingleThread { idleThreads ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val initialProjectConfig = ProjectConfig(
                "https://base-url.com",
                "project-token",
                "Token auth"
            )
            val deviceId = DeviceIdManager.getDeviceId(context)
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(
                context,
                ExponeaConfiguration(
                    integrationConfig = initialProjectConfig,
                    automaticSessionTracking = true
                )
            )
            val userId = Exponea.componentForTesting.customerIdsRepository.get().cookie
            Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
            )
            val newProjectConfig = ProjectConfig(
                "https://other-base-url.com",
                "new_project_token",
                "Token other-auth"
            )
            Exponea.anonymize(integrationConfig = newProjectConfig)
            val newUserId = Exponea.componentForTesting.customerIdsRepository.get().cookie
            Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
            )
            idleThreads()
            val events = Exponea.componentForTesting.eventRepository.all()
            events.sortedBy { it.timestamp }
            assertEquals(expected = 6, actual = events.size)
            checkEvent(events[0], installation, initialProjectConfig, userId!!, null)
            checkEvent(events[1], "test", initialProjectConfig, userId, expectedTestEventProperties(deviceId))
            checkEvent(events[2], Constants.EventTypes.sessionEnd, initialProjectConfig, userId, null)
            checkEvent(events[3], installation, newProjectConfig, newUserId!!, null)
            checkEvent(events[4], sessionStart, newProjectConfig, newUserId, null)
            checkEvent(events[5], "test", newProjectConfig, newUserId, expectedTestEventProperties(deviceId))
        }
    }

    @Test
    fun `should clear segmentation cache and processes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        val projectConfig = ProjectConfig("https://base-url.com", "project-token", "Token auth")
        Exponea.init(context, ExponeaConfiguration(integrationConfig = projectConfig, automaticSessionTracking = false))
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any<ProjectConfig>(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(
                Result(true, SegmentTest.getSegmentations())
            )
        }
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                // be there
            }
        })
        val segmentsManager = Exponea.componentForTesting.segmentsManager as SegmentsManagerImpl
        assertNotNull(segmentsManager.checkSegmentsJob)
        assertEquals(1, segmentsManager.newbieCallbacks.size)
        Exponea.anonymize()
        assertNull(Exponea.componentForTesting.segmentsCache.get())
        assertNull(segmentsManager.checkSegmentsJob)
        assertEquals(0, segmentsManager.newbieCallbacks.size)
        assertNull(Exponea.componentForTesting.segmentsCache.get())
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 10)
    }

    @Test
    fun `should auto flush before anonymize when StreamConfig and JWT is active`() = runInSingleThread { idleThreads ->
        val context = ApplicationProvider.getApplicationContext<Context>()
        val streamConfig = StreamConfig(streamId = "test-stream")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(integrationConfig = streamConfig))
        Exponea.componentForTesting.authTokenRepository.setToken("test-jwt")
        every { anyConstructed<FlushManagerImpl>().flushData(any()) } answers {
            firstArg<FlushFinishedCallback?>()?.invoke(kotlin.Result.success(Unit))
        }
        Exponea.anonymize()
        idleThreads()
        verify(exactly = 1) { anyConstructed<FlushManagerImpl>().flushData(any()) }
        assertThat(Exponea.componentForTesting.authTokenRepository.getToken(), nullValue())
    }

    @Test
    fun `should not flush before anonymize when ProjectConfig is used`() = runInSingleThread { idleThreads ->
        val context = ApplicationProvider.getApplicationContext<Context>()
        val projectConfig = ProjectConfig(projectToken = "project-token", authorization = "Token auth")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(integrationConfig = projectConfig))
        Exponea.anonymize()
        idleThreads()
        verify(exactly = 0) { anyConstructed<FlushManagerImpl>().flushData(any()) }
    }

    @Test
    fun `should not flush before anonymize when StreamConfig has no JWT and no auth callback`() =
        runInSingleThread { idleThreads ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val streamConfig = StreamConfig(streamId = "test-stream")
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(context, ExponeaConfiguration(integrationConfig = streamConfig))
            Exponea.anonymize()
            idleThreads()
            verify(exactly = 0) { anyConstructed<FlushManagerImpl>().flushData(any()) }
        }

    @Test
    fun `should auto flush before anonymize when StreamConfig and auth callback is set without token`() =
        runInSingleThread { idleThreads ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val streamConfig = StreamConfig(streamId = "test-stream")
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(context, ExponeaConfiguration(integrationConfig = streamConfig))
            Exponea.sdkAuthCallback = object : SdkAuthCallback {
                override fun onAuthFailure(error: SdkAuthError) {}
            }
            every { anyConstructed<FlushManagerImpl>().flushData(any()) } answers {
                firstArg<FlushFinishedCallback?>()?.invoke(kotlin.Result.success(Unit))
            }
            Exponea.anonymize()
            idleThreads()
            verify(exactly = 1) { anyConstructed<FlushManagerImpl>().flushData(any()) }
        }

    @Test
    fun `should invoke onAnonymized callback after completion`() = runInSingleThread { idleThreads ->
        val context = ApplicationProvider.getApplicationContext<Context>()
        val projectConfig = ProjectConfig(projectToken = "project-token", authorization = "Token auth")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(integrationConfig = projectConfig))
        var callbackInvoked = false
        Exponea.anonymize(null, null) { callbackInvoked = true }
        idleThreads()
        assertThat(callbackInvoked, equalTo(true))
    }

    @Test
    fun `should invoke onAnonymized callback after flush completes`() = runInSingleThread { idleThreads ->
        val context = ApplicationProvider.getApplicationContext<Context>()
        val streamConfig = StreamConfig(streamId = "test-stream")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(integrationConfig = streamConfig))
        Exponea.componentForTesting.authTokenRepository.setToken("test-jwt")
        var callbackInvoked = false
        every { anyConstructed<FlushManagerImpl>().flushData(any()) } answers {
            assertThat(
                "Callback should not be invoked before flush completes",
                callbackInvoked,
                equalTo(false)
            )
            firstArg<FlushFinishedCallback?>()?.invoke(kotlin.Result.success(Unit))
        }
        Exponea.anonymize(null, null) { callbackInvoked = true }
        idleThreads()
        assertThat(callbackInvoked, equalTo(true))
    }

    @Test
    fun `should warn when explicit StreamConfig is combined with integrationRouteMap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(
            context, ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "token", authorization = "Token auth")
            )
        )
        mockkObject(Logger)
        every { Logger.w(any(), any<String>()) } returns Unit
        val messages = mutableListOf<String>()
        val overrides = ExponeaConfigurationOverrides(
            integrationRouteMap = mapOf(
                EventType.TRACK_EVENT to listOf(
                    ProjectConfig(projectToken = "other", authorization = "Token other")
                )
            )
        )

        Exponea.anonymize(
            integrationConfig = StreamConfig(streamId = "stream"),
            exponeaConfigurationOverrides = overrides
        )

        verify { Logger.w(any(), capture(messages)) }
        assertThat(
            messages,
            hasItem(
                "Integration route mapping is only supported when using ProjectConfig. " +
                        "This setting will be ignored for StreamConfig."
            )
        )
        unmockkObject(Logger)
    }

    @Test
    fun `should warn when inherited StreamConfig is combined with integrationRouteMap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(integrationConfig = StreamConfig(streamId = "stream")))
        mockkObject(Logger)
        every { Logger.w(any(), any<String>()) } returns Unit
        val messages = mutableListOf<String>()
        val overrides = ExponeaConfigurationOverrides(
            integrationRouteMap = mapOf(
                EventType.TRACK_EVENT to listOf(
                    ProjectConfig(
                        projectToken = "other", authorization = "Token other"
                    )
                )
            )
        )

        Exponea.anonymize(
            integrationConfig = null,
            exponeaConfigurationOverrides = overrides
        )

        verify { Logger.w(any(), capture(messages)) }
        assertThat(
            messages,
            hasItem(
                "Integration route mapping is only supported when using ProjectConfig. " +
                        "This setting will be ignored for StreamConfig."
            )
        )
        unmockkObject(Logger)
    }

    @Test
    fun `should complete anonymize even when flush fails`() = runInSingleThread { idleThreads ->
        val context = ApplicationProvider.getApplicationContext<Context>()
        val streamConfig = StreamConfig(streamId = "test-stream")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(integrationConfig = streamConfig))
        Exponea.componentForTesting.authTokenRepository.setToken("test-jwt")
        every { anyConstructed<FlushManagerImpl>().flushData(any()) } answers {
            firstArg<FlushFinishedCallback?>()?.invoke(
                kotlin.Result.failure(Exception("No internet connection"))
            )
        }
        var callbackInvoked = false
        Exponea.anonymize(null, null) { callbackInvoked = true }
        idleThreads()
        assertThat(
            "onAnonymized should be invoked even when flush fails",
            callbackInvoked,
            equalTo(true)
        )
    }

    @Test
    fun `should preserve device_id across anonymize when regenerateDeviceIdOnAnonymize is false`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val projectConfig = ProjectConfig("https://base-url.com", "project-token", "Token auth")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(
            context,
            ExponeaConfiguration(
                integrationConfig = projectConfig,
                regenerateDeviceIdOnAnonymize = false
            )
        )
        val deviceIdBefore = DeviceIdManager.getDeviceId(context)
        Exponea.anonymize()
        val deviceIdAfter = DeviceIdManager.getDeviceId(context)
        assertEquals(deviceIdBefore, deviceIdAfter)
    }

    @Test
    fun `should regenerate device_id on anonymize when regenerateDeviceIdOnAnonymize is true`() =
        runInSingleThread { idleThreads ->
            mockkObject(NotificationsPermissionReceiver)
            every { NotificationsPermissionReceiver.isPermissionGranted(any()) } returns true
            val context = ApplicationProvider.getApplicationContext<Context>()
            val projectConfig = ProjectConfig("https://base-url.com", "project-token", "Token auth")
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(
                context,
                ExponeaConfiguration(
                    integrationConfig = projectConfig,
                    automaticSessionTracking = false,
                    regenerateDeviceIdOnAnonymize = true
                )
            )
            Exponea.trackPushToken("push_token")
            val deviceIdBefore = DeviceIdManager.getDeviceId(context)
            Exponea.anonymize()
            idleThreads()
            val deviceIdAfter = DeviceIdManager.getDeviceId(context)

            assertNotEquals(deviceIdBefore, deviceIdAfter)

            val events = Exponea.componentForTesting.eventRepository.all()
            // events[0]: installation        (old user) — OLD device_id
            // events[1]: pushTokenTrack valid=true (old user) — OLD device_id
            // events[2]: pushTokenTrack valid=false INVALIDATED (old user) — OLD device_id (before clear)
            // events[3]: installation        (new user) — NEW device_id
            // events[4]: pushTokenTrack valid=true (new user) — NEW device_id
            assertEquals(5, events.size)
            assertEquals(deviceIdBefore, events[2].properties?.get("device_id"))
            assertEquals(deviceIdAfter, events[3].properties?.get("device_id"))
            assertEquals(deviceIdAfter, events[4].properties?.get("device_id"))
        }
}
