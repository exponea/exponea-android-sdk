package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageDisplayState
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.IntegrationConfigType
import com.exponea.sdk.models.IntegrationConfiguration
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.Route
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.repository.VolatileInAppMessagesETagStore
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.services.IntegrationConfigFactory
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.view.InAppMessagePresenter
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EventManagerTest : ExponeaSDKTest() {
    lateinit var eventRepo: EventRepository
    lateinit var flushManager: FlushManager
    lateinit var inAppMessageManager: InAppMessageManagerImpl
    lateinit var fetchManager: FetchManager
    lateinit var customerIdsRepository: CustomerIdsRepository
    lateinit var inAppMessageDisplayStateRepository: InAppMessageDisplayStateRepository
    lateinit var messagesCache: InAppMessagesCache
    lateinit var drawableCache: DrawableCache
    lateinit var fontCache: FontCache
    lateinit var presenter: InAppMessagePresenter
    lateinit var trackingConsentManager: TrackingConsentManager
    lateinit var manager: EventManagerImpl
    lateinit var projectFactory: IntegrationConfigFactory
    lateinit var addedEvents: ArrayList<ExportedEvent>
    lateinit var deviceId: String

    fun setup(context: Context, configuration: ExponeaConfiguration, flushMode: FlushMode) {
        deviceId = DeviceIdManager.getDeviceId(context)
        mockkObject(Exponea)
        every { Exponea.flushMode } returns flushMode

        eventRepo = mockk()
        addedEvents = arrayListOf()
        every { eventRepo.add(capture(addedEvents)) } just Runs

        val customerIdsRepo = mockk<CustomerIdsRepository>()
        every { customerIdsRepo.get() } returns CustomerIds(cookie = "mock-cookie")

        flushManager = mockk()
        every { flushManager.flushData(any()) } just Runs

        fetchManager = mockk()
        every {
            fetchManager.fetchInAppMessages(
                any<ProjectConfig>(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            arg<(Result<List<InAppMessage>>) -> Unit>(5).invoke(
                Result(true, arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle()))
            )
        }
        messagesCache = mockk()
        every { messagesCache.set(any()) } just Runs
        every { messagesCache.getTimestamp() } returns System.currentTimeMillis()
        every { messagesCache.get() } returns arrayListOf()
        drawableCache = mockk()
        every { drawableCache.has(any()) } returns false
        every { drawableCache.preload(any(), any()) } just Runs
        every { drawableCache.clear() } just Runs
        fontCache = mockk()
        every { fontCache.has(any()) } returns false
        every { fontCache.preload(any(), any()) } just Runs
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds()
        inAppMessageDisplayStateRepository = mockk()
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(null, null)
        every { inAppMessageDisplayStateRepository.setDisplayed(any(), any()) } just Runs
        every { inAppMessageDisplayStateRepository.setInteracted(any(), any()) } just Runs
        presenter = mockk()
        every { presenter.show(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        every { presenter.isPresenting() } returns false
        trackingConsentManager = mockk()
        every { trackingConsentManager.trackInAppMessageError(any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageClose(any(), any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageClick(any(), any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageShown(any(), any()) } just Runs
        projectFactory = IntegrationConfigFactory(configuration)
        inAppMessageManager = spyk(
            InAppMessageManagerImpl(
                customerIdsRepo,
                messagesCache,
                fetchManager,
                inAppMessageDisplayStateRepository,
                drawableCache,
                fontCache,
                presenter,
                trackingConsentManager,
                projectFactory,
                VolatileInAppMessagesETagStore()
            )
        )
        every { inAppMessageManager.sessionStarted(any()) } just Runs

        manager = EventManagerImpl(
            configuration,
            eventRepo,
            customerIdsRepo,
            flushManager,
            projectFactory,
            onEventCreated = { event, type ->
                inAppMessageManager.onEventCreated(event, type)
            },
            deviceIdProvider = { DeviceIdManager.getDeviceId(context = context) }
        )
    }

    @Test
    fun `should track event`() = runInSingleThread { idleThreads ->
        ExponeaContextProvider.applicationIsForeground = true
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(integrationConfig = ProjectConfig(projectToken = "mock-project-token")),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        Robolectric.flushForegroundThreadScheduler()
        idleThreads()
        verify {
            eventRepo.add(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.detectReloadMode(any(), any(), any())
            inAppMessageManager.pickAndShowMessage()
            inAppMessageManager.pickPendingMessage()
            inAppMessageManager.findMessagesByFilter(any(), any(), any())
            inAppMessageManager.pendingShowRequests
            inAppMessageManager.pendingShowRequests = any()
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
        val firstAddedEvent = addedEvents.first()
        assertEquals(
            ExportedEvent(
                id = firstAddedEvent.id,
                type = "test-event",
                timestamp = 123.0,
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value",
                    "application_id" to "default-application",
                    "device_id" to deviceId
                ),
                integrationConfiguration = IntegrationConfiguration(
                    "mock-project-token",
                    "https://api.exponea.com",
                    null,
                    IntegrationConfigType.PROJECT
                ),
                route = Route.TRACK_EVENTS,
                sdkEventType = EventType.TRACK_EVENT.name
            ),
            firstAddedEvent
        )
    }

    @Test
    fun `should track event for all projects`() = runInSingleThread { idleThreads ->
        ExponeaContextProvider.applicationIsForeground = true
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                integrationRouteMap = hashMapOf(
                    EventType.INSTALL to arrayListOf(
                        ProjectConfig("mock_base_url1.com", "token1", "mock_auth"),
                        ProjectConfig("mock_base_url2.com", "token2", "mock_auth")
                    )
                )
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.INSTALL)
        Robolectric.flushForegroundThreadScheduler()
        idleThreads()
        verify {
            eventRepo.add(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.detectReloadMode(any(), any(), any())
            inAppMessageManager.pickAndShowMessage()
            inAppMessageManager.pickPendingMessage()
            inAppMessageManager.findMessagesByFilter(any(), any(), any())
            inAppMessageManager.pendingShowRequests
            inAppMessageManager.pendingShowRequests = any()
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
        assertEquals(3, addedEvents.size)
        assertEquals(
            IntegrationConfiguration(
                "mock-project-token",
                "https://api.exponea.com",
                null,
                IntegrationConfigType.PROJECT
            ),
            addedEvents[0].integrationConfiguration
        )
        assertEquals(
            IntegrationConfiguration("token1", "mock_base_url1.com", "mock_auth", IntegrationConfigType.PROJECT),
            addedEvents[1].integrationConfiguration
        )
        assertEquals(
            IntegrationConfiguration("token2", "mock_base_url2.com", "mock_auth", IntegrationConfigType.PROJECT),
            addedEvents[2].integrationConfiguration
        )
    }

    @Test
    fun `should start flush in immediate flush mode`() = runInSingleThread { idleThreads ->
        ExponeaContextProvider.applicationIsForeground = true
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(integrationConfig = ProjectConfig(projectToken = "mock-project-token")),
            FlushMode.IMMEDIATE
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        Robolectric.flushForegroundThreadScheduler()
        idleThreads()
        verify {
            eventRepo.add(any())
            flushManager.flushData(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.detectReloadMode(any(), any(), any())
            inAppMessageManager.pickAndShowMessage()
            inAppMessageManager.pickPendingMessage()
            inAppMessageManager.findMessagesByFilter(any(), any(), any())
            inAppMessageManager.pendingShowRequests
            inAppMessageManager.pendingShowRequests = any()
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
    }

    @Test
    fun `should notify in-app message manager of session start`() = runInSingleThread { idleThreads ->
        ExponeaContextProvider.applicationIsForeground = true
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(integrationConfig = ProjectConfig(projectToken = "mock-project-token")),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.SESSION_START)
        Robolectric.flushForegroundThreadScheduler()
        idleThreads()
        verify {
            eventRepo.add(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.sessionStarted(any())
            inAppMessageManager.detectReloadMode(any(), any(), any())
            inAppMessageManager.pickAndShowMessage()
            inAppMessageManager.pickPendingMessage()
            inAppMessageManager.findMessagesByFilter(any(), any(), any())
            inAppMessageManager.pendingShowRequests
            inAppMessageManager.pendingShowRequests = any()
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
    }

    @Test
    fun `should add default properties`() = runInSingleThread { idleThreads ->
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2")
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        idleThreads()
        val firstAddedEvent = addedEvents.first()
        assertEquals(
            ExportedEvent(
                id = firstAddedEvent.id,
                type = "test-event",
                timestamp = 123.0,
                integrationConfiguration = IntegrationConfiguration(
                    baseUrl = "https://api.exponea.com",
                    integrationId = "mock-project-token",
                    authorization = null,
                    type = IntegrationConfigType.PROJECT
                ),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value",
                    "default-prop1" to "value1",
                    "default-prop2" to "value2",
                    "application_id" to "default-application",
                    "device_id" to deviceId
                ),
                route = Route.TRACK_EVENTS,
                sdkEventType = EventType.TRACK_EVENT.name
            ), firstAddedEvent
        )
    }

    @Test
    fun `should not accumulate default properties`() = runInSingleThread { idleThreads ->
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2")
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        idleThreads()
        val firstEvent = addedEvents[0]
        val secondEvent = addedEvents[1]
        assertEquals(
            ExportedEvent(
                id = firstEvent.id,
                type = "test-event",
                timestamp = 123.0,
                route = Route.TRACK_EVENTS,
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value",
                    "default-prop1" to "value1",
                    "default-prop2" to "value2",
                    "application_id" to "default-application",
                    "device_id" to deviceId
                ),
                integrationConfiguration = IntegrationConfiguration(
                    baseUrl = "https://api.exponea.com",
                    integrationId = "mock-project-token",
                    authorization = null,
                    type = IntegrationConfigType.PROJECT
                ),
                sdkEventType = EventType.TRACK_EVENT.name
            ),
            firstEvent
        )
        assertEquals(
            ExportedEvent(
                id = secondEvent.id,
                type = "test-event",
                timestamp = 123.0,
                route = Route.TRACK_EVENTS,
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value",
                    "default-prop1" to "value1",
                    "default-prop2" to "value2",
                    "application_id" to "default-application",
                    "device_id" to deviceId
                ),
                integrationConfiguration = IntegrationConfiguration(
                    baseUrl = "https://api.exponea.com",
                    integrationId = "mock-project-token",
                    authorization = null,
                    type = IntegrationConfigType.PROJECT
                ),
                sdkEventType = EventType.TRACK_EVENT.name
            ),
            secondEvent
        )
    }

    @Test
    fun `should add default properties to customer properties by default`() = runInSingleThread { idleThreads ->
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2")
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_CUSTOMER)
        idleThreads()
        val firstAddedEvent = addedEvents.first()
        assertEquals(
            ExportedEvent(
                id = firstAddedEvent.id,
                type = "test-event",
                timestamp = 123.0,
                integrationConfiguration = IntegrationConfiguration(
                    baseUrl = "https://api.exponea.com",
                    integrationId = "mock-project-token",
                    authorization = null,
                    type = IntegrationConfigType.PROJECT
                ),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value",
                    "default-prop1" to "value1",
                    "default-prop2" to "value2"
                ),
                route = Route.TRACK_CUSTOMERS,
                sdkEventType = EventType.TRACK_CUSTOMER.name
            ), firstAddedEvent
        )
    }

    @Test
    fun `should add default properties to customer properties if allowed`() = runInSingleThread { idleThreads ->
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2"),
                allowDefaultCustomerProperties = true
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_CUSTOMER)
        idleThreads()
        val firstAddedEvent = addedEvents.first()
        assertEquals(
            ExportedEvent(
                id = firstAddedEvent.id,
                type = "test-event",
                timestamp = 123.0,
                integrationConfiguration = IntegrationConfiguration(
                    baseUrl = "https://api.exponea.com",
                    integrationId = "mock-project-token",
                    authorization = null,
                    type = IntegrationConfigType.PROJECT
                ),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value",
                    "default-prop1" to "value1",
                    "default-prop2" to "value2"
                ),
                route = Route.TRACK_CUSTOMERS,
                sdkEventType = EventType.TRACK_CUSTOMER.name
            ), firstAddedEvent
        )
    }

    @Test
    fun `should NOT add default properties to customer properties if denied`() = runInSingleThread { idleThreads ->
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2"),
                allowDefaultCustomerProperties = false
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_CUSTOMER)
        idleThreads()
        val firstAddedEvent = addedEvents.first()
        assertEquals(
            ExportedEvent(
                id = firstAddedEvent.id,
                type = "test-event",
                timestamp = 123.0,
                integrationConfiguration = IntegrationConfiguration(
                    baseUrl = "https://api.exponea.com",
                    integrationId = "mock-project-token",
                    authorization = null,
                    type = IntegrationConfigType.PROJECT
                ),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value"
                ),
                route = Route.TRACK_CUSTOMERS,
                sdkEventType = EventType.TRACK_CUSTOMER.name
            ), firstAddedEvent
        )
    }

    @Test
    fun `should NOT add default properties to customer properties if allowed - push token update`() {
        runInSingleThread { idleThreads ->
            setup(
                ApplicationProvider.getApplicationContext(),
                ExponeaConfiguration(
                    integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                    defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2"),
                    allowDefaultCustomerProperties = true
                ),
                FlushMode.MANUAL
            )
            manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.PUSH_TOKEN)
            idleThreads()
            val firstAddedEvent = addedEvents.first()
            assertEquals(
                ExportedEvent(
                    id = firstAddedEvent.id,
                    type = "test-event",
                    timestamp = 123.0,
                    integrationConfiguration = IntegrationConfiguration(
                        baseUrl = "https://api.exponea.com",
                        integrationId = "mock-project-token",
                        authorization = null,
                        type = IntegrationConfigType.PROJECT
                    ),
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    properties = hashMapOf(
                        "prop" to "value",
                        "application_id" to "default-application",
                        "device_id" to deviceId
                    ),
                    route = Route.TRACK_EVENTS,
                    sdkEventType = EventType.PUSH_TOKEN.name
                ), firstAddedEvent
            )
        }
    }

    @Test
    fun `should NOT add default properties to customer properties if denied - push token update`() {
        runInSingleThread { idleThreads ->
            setup(
                ApplicationProvider.getApplicationContext(),
                ExponeaConfiguration(
                    integrationConfig = ProjectConfig(projectToken = "mock-project-token"),
                    defaultProperties = hashMapOf("default-prop1" to "value1", "default-prop2" to "value2"),
                    allowDefaultCustomerProperties = false
                ),
                FlushMode.MANUAL
            )
            manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.PUSH_TOKEN)
            idleThreads()
            val firstAddedEvent = addedEvents.first()
            assertEquals(
                ExportedEvent(
                    id = firstAddedEvent.id,
                    type = "test-event",
                    timestamp = 123.0,
                    integrationConfiguration = IntegrationConfiguration(
                        baseUrl = "https://api.exponea.com",
                        integrationId = "mock-project-token",
                        authorization = null,
                        type = IntegrationConfigType.PROJECT
                    ),
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    properties = hashMapOf(
                        "prop" to "value",
                        "application_id" to "default-application",
                        "device_id" to deviceId
                    ),
                    route = Route.TRACK_EVENTS,
                    sdkEventType = EventType.PUSH_TOKEN.name
                ), firstAddedEvent
            )
        }
    }

    @Test
    fun `should invoke flush only after event is stored for immediate flush`() {
        ExponeaContextProvider.applicationIsForeground = true
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = ProjectConfig(projectToken = "mock-project-token")
            ),
            FlushMode.IMMEDIATE
        )
        var eventAddedAt = 0L
        var flushedAt = 0L
        waitForIt(3000) { done ->
            every { eventRepo.add(any()) } answers {
                Thread.sleep(2000)
                eventAddedAt = System.currentTimeMillis()
            }
            every { flushManager.flushData(any()) } answers {
                flushedAt = System.currentTimeMillis()
                done()
            }
            manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        }
        assertNotEquals(0, eventAddedAt)
        assertNotEquals(0, flushedAt)
        assertTrue(eventAddedAt <= flushedAt)
    }

    @Test
    fun `should not apply integration route map for StreamConfig`() = runInSingleThread { idleThreads ->
        ExponeaContextProvider.applicationIsForeground = true
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                integrationConfig = StreamConfig(streamId = "mock-stream-id"),
                integrationRouteMap = hashMapOf(
                    EventType.INSTALL to arrayListOf(
                        ProjectConfig("mock_base_url1.com", "token1", "mock_auth")
                    )
                )
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.INSTALL)
        Robolectric.flushForegroundThreadScheduler()
        idleThreads()

        // Should only create one event for StreamConfig, ignoring the integrationRouteMap
        assertEquals(1, addedEvents.size)
        assertEquals(
            IntegrationConfiguration(
                "mock-stream-id",
                "https://api.exponea.com",
                null,
                IntegrationConfigType.STREAM
            ),
            addedEvents[0].integrationConfiguration
        )
    }

    @Test
    fun `should track event with StreamConfig`() = runInSingleThread { idleThreads ->
        ExponeaContextProvider.applicationIsForeground = true
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(integrationConfig = StreamConfig(streamId = "mock-stream-id")),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        Robolectric.flushForegroundThreadScheduler()
        idleThreads()

        assertEquals(1, addedEvents.size)
        val firstAddedEvent = addedEvents.first()
        assertEquals(
            ExportedEvent(
                id = firstAddedEvent.id,
                type = "test-event",
                timestamp = 123.0,
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf(
                    "prop" to "value",
                    "application_id" to "default-application",
                    "device_id" to deviceId
                ),
                integrationConfiguration = IntegrationConfiguration(
                    "mock-stream-id",
                    "https://api.exponea.com",
                    null,
                    IntegrationConfigType.STREAM
                ),
                route = Route.TRACK_EVENTS,
                sdkEventType = EventType.TRACK_EVENT.name
            ),
            firstAddedEvent
        )
    }
}
