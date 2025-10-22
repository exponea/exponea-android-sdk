package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.DeviceIdManager
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.SegmentsManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.util.currentTimeSeconds
import io.mockk.every
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AnonymizeTest : ExponeaSDKTest() {

    private fun expectedPushTokenProperties(
        pushToken: String = "push_token",
        platform: String = "android",
        applicationId: String = "default-application",
        valid: Boolean,
        description: String,
        deviceId: String
    ): HashMap<String, Any> =
        hashMapOf(
            "push_notification_token" to pushToken,
            "platform" to platform,
            "application_id" to applicationId,
            "valid" to valid,
            "description" to description,
            "device_id" to deviceId
        )

    private fun expectedTestEventProperties(deviceId: String): HashMap<String, Any> =
        hashMapOf(
            "name" to "test",
            "application_id" to "default-application",
            "device_id" to deviceId
        )

    private fun checkEvent(
        event: ExportedEvent,
        expectedEventType: String?,
        expectedProject: ExponeaProject,
        expectedUserId: String,
        expectedProperties: HashMap<String, Any>? = null
    ) {
        assertEquals(expectedEventType, event.type)
        assertEquals(expectedProject, event.exponeaProject)
        assertEquals(hashMapOf<String, String?>("cookie" to expectedUserId), event.customerIds)
        if (expectedProperties != null) assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `should anonymize sdk and switch projects`() = runInSingleThread { idleThreads ->
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialProject = ExponeaProject("https://base-url.com", "project-token", "Token auth")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(
            baseURL = initialProject.baseUrl,
            projectToken = initialProject.projectToken,
            authorization = initialProject.authorization)
        )
        val testFirebaseToken = "push_token"
        val userId = Exponea.componentForTesting.customerIdsRepository.get().cookie

        Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
        )
        Exponea.trackPushToken(testFirebaseToken)

        val newProject = ExponeaProject("https://other-base-url.com", "new_project_token", "Token other-auth")
        Exponea.anonymize(exponeaProject = newProject)
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
        checkEvent(events[0], Constants.EventTypes.installation, initialProject, userId!!, null)
        checkEvent(events[1], "test", initialProject, userId, expectedTestEventProperties(deviceId))
        checkEvent(events[2], Constants.EventTypes.pushTokenTrack, initialProject, userId, expectedPushTokenProperties(
            valid = true,
            description = Constants.PushPermissionStatus.PERMISSION_GRANTED,
            deviceId = deviceId
        ))
        checkEvent(events[3], Constants.EventTypes.sessionEnd, initialProject, userId, null)
        // anonymize is called. We clear push token in old user and track initial events for new user
        checkEvent(events[4], Constants.EventTypes.pushTokenTrack, initialProject, userId, expectedPushTokenProperties(
            valid = false,
            description = Constants.PushPermissionStatus.INVALIDATED_TOKEN,
            deviceId = deviceId
        ))
        checkEvent(events[5], Constants.EventTypes.installation, newProject, newUserId!!, null)
        checkEvent(events[6], Constants.EventTypes.sessionStart, newProject, newUserId, null)
        checkEvent(events[7], Constants.EventTypes.pushTokenTrack, newProject, newUserId, expectedPushTokenProperties(
            valid = true,
            description = Constants.PushPermissionStatus.PERMISSION_GRANTED,
            deviceId = deviceId
        ))
        checkEvent(events[8], "test", newProject, newUserId, expectedTestEventProperties(deviceId))
    }

    @Test
    fun `should not track session start on anonymize when automaticSessionTracking is off`() {
        runInSingleThread { idleThreads ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val initialProject = ExponeaProject("https://base-url.com", "project-token", "Token auth")
            val deviceId = DeviceIdManager.getDeviceId(context)
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(context, ExponeaConfiguration(
                baseURL = initialProject.baseUrl,
                projectToken = initialProject.projectToken,
                authorization = initialProject.authorization,
                automaticSessionTracking = false)
            )
            val userId = Exponea.componentForTesting.customerIdsRepository.get().cookie
            Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
            )
            val newProject = ExponeaProject("https://other-base-url.com", "new_project_token", "Token other-auth")
            Exponea.anonymize(exponeaProject = newProject)
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
            checkEvent(events[0], Constants.EventTypes.installation, initialProject, userId!!, null)
            checkEvent(events[1], "test", initialProject, userId, expectedTestEventProperties(deviceId))
            checkEvent(events[2], Constants.EventTypes.installation, newProject, newUserId!!, null)
            checkEvent(events[3], "test", newProject, newUserId, expectedTestEventProperties(deviceId))
        }
    }

    @Test
    fun `should track session start and end on anonymize when automaticSessionTracking is on`() {
        runInSingleThread { idleThreads ->
            val context = ApplicationProvider.getApplicationContext<Context>()
            val initialProject = ExponeaProject("https://base-url.com", "project-token", "Token auth")
            val deviceId = DeviceIdManager.getDeviceId(context)
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(context, ExponeaConfiguration(
                baseURL = initialProject.baseUrl,
                projectToken = initialProject.projectToken,
                authorization = initialProject.authorization,
                automaticSessionTracking = true)
            )
            val userId = Exponea.componentForTesting.customerIdsRepository.get().cookie
            Exponea.trackEvent(
                eventType = "test",
                properties = PropertiesList(hashMapOf("name" to "test")),
                timestamp = currentTimeSeconds()
            )
            val newProject = ExponeaProject("https://other-base-url.com", "new_project_token", "Token other-auth")
            Exponea.anonymize(exponeaProject = newProject)
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
            checkEvent(events[0], Constants.EventTypes.installation, initialProject, userId!!, null)
            checkEvent(events[1], "test", initialProject, userId, expectedTestEventProperties(deviceId))
            checkEvent(events[2], Constants.EventTypes.sessionEnd, initialProject, userId, null)
            checkEvent(events[3], Constants.EventTypes.installation, newProject, newUserId!!, null)
            checkEvent(events[4], Constants.EventTypes.sessionStart, newProject, newUserId, null)
            checkEvent(events[5], "test", newProject, newUserId, expectedTestEventProperties(deviceId))
        }
    }

    @Test
    fun `should clear segmentation cache and processes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        val project = ExponeaProject("https://base-url.com", "project-token", "Token auth")
        Exponea.init(context, ExponeaConfiguration(
                baseURL = project.baseUrl,
                projectToken = project.projectToken,
                authorization = project.authorization,
                automaticSessionTracking = false)
        )
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) } answers {
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
}
