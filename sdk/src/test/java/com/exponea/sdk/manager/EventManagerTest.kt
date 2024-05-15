package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageDisplayState
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.Route
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.repository.SimpleFileCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
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
    lateinit var fontCache: SimpleFileCache
    lateinit var presenter: InAppMessagePresenter
    lateinit var trackingConsentManager: TrackingConsentManager
    lateinit var manager: EventManagerImpl
    lateinit var projectFactory: ExponeaProjectFactory

    lateinit var addedEvents: ArrayList<ExportedEvent>

    fun setup(context: Context, configuration: ExponeaConfiguration, flushMode: FlushMode) {
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
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(InAppMessageTest.getInAppMessage()))
            )
        }
        messagesCache = mockk()
        every { messagesCache.set(any()) } just Runs
        every { messagesCache.getTimestamp() } returns System.currentTimeMillis()
        every { messagesCache.get() } returns arrayListOf()
        drawableCache = mockk()
        every { drawableCache.has(any()) } returns false
        every { drawableCache.preload(any(), any()) } just Runs
        every { drawableCache.clearExcept(any()) } just Runs
        fontCache = mockk()
        every { fontCache.has(any()) } returns false
        every { fontCache.preload(any(), any()) } just Runs
        every { fontCache.clearExcept(any()) } just Runs
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds()
        inAppMessageDisplayStateRepository = mockk()
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(null, null)
        every { inAppMessageDisplayStateRepository.setDisplayed(any(), any()) } just Runs
        every { inAppMessageDisplayStateRepository.setInteracted(any(), any()) } just Runs
        presenter = mockk()
        every { presenter.show(any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        every { presenter.isPresenting() } returns false
        trackingConsentManager = mockk()
        every { trackingConsentManager.trackInAppMessageError(any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageClose(any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageClick(any(), any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageShown(any(), any()) } just Runs
        projectFactory = ExponeaProjectFactory(context, configuration)
        inAppMessageManager = spyk(InAppMessageManagerImpl(
            customerIdsRepo,
            messagesCache,
            fetchManager,
            inAppMessageDisplayStateRepository,
            drawableCache,
            fontCache,
            presenter,
            trackingConsentManager,
            projectFactory
        ))
        every { inAppMessageManager.sessionStarted(any()) } just Runs

        manager = EventManagerImpl(
            configuration,
            eventRepo,
            customerIdsRepo,
            flushManager,
            projectFactory,
            onEventCreated = { event, type ->
                inAppMessageManager.onEventCreated(event, type)
            }
        )
    }

    @Before
    fun overrideThreadBehaviour() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Main)
    }

    @After
    fun restoreThreadBehaviour() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Default)
    }

    @Test
    fun `should track event`() {
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(projectToken = "mock-project-token"),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        Robolectric.flushForegroundThreadScheduler()
        verify {
            eventRepo.add(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.detectReloadMode(any(), any())
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
                properties = hashMapOf("prop" to "value"),
                exponeaProject = ExponeaProject(
                        "https://api.exponea.com",
                        "mock-project-token",
                        null),
                projectId = "mock-project-token",
                route = Route.TRACK_EVENTS,
                sdkEventType = EventType.TRACK_EVENT.name
            ),
            firstAddedEvent
        )
    }

    @Test
    fun `should track event for all projects`() {
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                projectToken = "mock-project-token",
                projectRouteMap = hashMapOf(EventType.INSTALL to arrayListOf(
                    ExponeaProject("mock_base_url1.com", "token1", "mock_auth"),
                    ExponeaProject("mock_base_url2.com", "token2", "mock_auth")
                ))
            ),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.INSTALL)
        Robolectric.flushForegroundThreadScheduler()
        verify {
            eventRepo.add(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.detectReloadMode(any(), any())
            inAppMessageManager.pickAndShowMessage()
            inAppMessageManager.pickPendingMessage()
            inAppMessageManager.findMessagesByFilter(any(), any(), any())
            inAppMessageManager.pendingShowRequests
            inAppMessageManager.pendingShowRequests = any()
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
        assertEquals(3, addedEvents.size)
        assertEquals(
            ExponeaProject("https://api.exponea.com", "mock-project-token", null),
            addedEvents[0].exponeaProject
        )
        assertEquals(
            ExponeaProject("mock_base_url1.com", "token1", "mock_auth"),
            addedEvents[1].exponeaProject
        )
        assertEquals(
            ExponeaProject("mock_base_url2.com", "token2", "mock_auth"),
            addedEvents[2].exponeaProject
        )
    }

    @Test
    fun `should start flush in immediate flush mode`() {
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(projectToken = "mock-project-token"),
            FlushMode.IMMEDIATE
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        Robolectric.flushForegroundThreadScheduler()
        verify {
            eventRepo.add(any())
            flushManager.flushData(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.detectReloadMode(any(), any())
            inAppMessageManager.pickAndShowMessage()
            inAppMessageManager.pickPendingMessage()
            inAppMessageManager.findMessagesByFilter(any(), any(), any())
            inAppMessageManager.pendingShowRequests
            inAppMessageManager.pendingShowRequests = any()
        }
        confirmVerified(eventRepo, flushManager, inAppMessageManager)
    }

    @Test
    fun `should notify in-app message manager of session start`() {
        setup(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(projectToken = "mock-project-token"),
            FlushMode.MANUAL
        )
        manager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.SESSION_START)
        Robolectric.flushForegroundThreadScheduler()
        verify {
            eventRepo.add(any())
            inAppMessageManager.onEventCreated(any(), any())
            inAppMessageManager.inAppShowingTriggered(any(), any(), any(), any(), any())
            inAppMessageManager.registerPendingShowRequest(any(), any(), any(), any())
            inAppMessageManager.sessionStarted(any())
            inAppMessageManager.detectReloadMode(any(), any())
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
                projectToken = "mock-project-token",
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
                exponeaProject = ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "mock-project-token",
                        authorization = null),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2"),
                projectId = "mock-project-token",
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
                projectToken = "mock-project-token",
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
                properties = hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2"),
                projectId = "mock-project-token",
                exponeaProject = ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "mock-project-token",
                        authorization = null),
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
                            "default-prop2" to "value2"),
                    exponeaProject = ExponeaProject(
                            baseUrl = "https://api.exponea.com",
                            projectToken = "mock-project-token",
                            authorization = null),
                    projectId = "mock-project-token",
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
                projectToken = "mock-project-token",
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
                exponeaProject = ExponeaProject(
                    baseUrl = "https://api.exponea.com",
                    projectToken = "mock-project-token",
                    authorization = null),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2"),
                projectId = "mock-project-token",
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
                projectToken = "mock-project-token",
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
                exponeaProject = ExponeaProject(
                    baseUrl = "https://api.exponea.com",
                    projectToken = "mock-project-token",
                    authorization = null),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2"),
                projectId = "mock-project-token",
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
                projectToken = "mock-project-token",
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
                exponeaProject = ExponeaProject(
                    baseUrl = "https://api.exponea.com",
                    projectToken = "mock-project-token",
                    authorization = null),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                properties = hashMapOf("prop" to "value"),
                projectId = "mock-project-token",
                route = Route.TRACK_CUSTOMERS,
                sdkEventType = EventType.TRACK_CUSTOMER.name
            ), firstAddedEvent
        )
    }

    @Test
    fun `should add default properties to customer properties if allowed - push token update`() {
        runInSingleThread { idleThreads ->
            setup(
                ApplicationProvider.getApplicationContext(),
                ExponeaConfiguration(
                    projectToken = "mock-project-token",
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
                    exponeaProject = ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "mock-project-token",
                        authorization = null),
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    properties = hashMapOf("prop" to "value", "default-prop1" to "value1", "default-prop2" to "value2"),
                    projectId = "mock-project-token",
                    route = Route.TRACK_CUSTOMERS,
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
                    projectToken = "mock-project-token",
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
                    exponeaProject = ExponeaProject(
                        baseUrl = "https://api.exponea.com",
                        projectToken = "mock-project-token",
                        authorization = null),
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    properties = hashMapOf("prop" to "value"),
                    projectId = "mock-project-token",
                    route = Route.TRACK_CUSTOMERS,
                    sdkEventType = EventType.PUSH_TOKEN.name
                ), firstAddedEvent
            )
        }
    }
}
