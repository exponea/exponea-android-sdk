package com.exponea.sdk.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.manager.TrackingConsentManager.MODE.CONSIDER_CONSENT
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DateFilter
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButton
import com.exponea.sdk.models.InAppMessageCallback
import com.exponea.sdk.models.InAppMessageDisplayState
import com.exponea.sdk.models.InAppMessageFrequency
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.eventfilter.EventFilter
import com.exponea.sdk.models.eventfilter.EventPropertyFilter
import com.exponea.sdk.models.eventfilter.StringConstraint
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.upload.SentryTelemetryUpload
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.runOnBackgroundThread
import com.exponea.sdk.view.InAppMessagePresenter
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageManagerImplTest {
    private lateinit var fetchManager: FetchManager
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var inAppMessageDisplayStateRepository: InAppMessageDisplayStateRepository
    private lateinit var messagesCache: InAppMessagesCache
    private lateinit var drawableCache: DrawableCache
    private lateinit var fontCache: FontCache
    private lateinit var presenter: InAppMessagePresenter
    private lateinit var manager: InAppMessageManagerImpl
    private lateinit var mockActivity: Activity
    private lateinit var trackingConsentManager: TrackingConsentManager
    private lateinit var projectFactory: ExponeaProjectFactory

    @Before
    fun disableTelemetry() {
        mockkConstructorFix(SentryTelemetryUpload::class) {
            every { anyConstructed<SentryTelemetryUpload>().sendSentryEnvelope(any(), any()) }
        }
        every { anyConstructed<SentryTelemetryUpload>().sendSentryEnvelope(any(), any()) } answers {
            secondArg<(kotlin.Result<Unit>) -> Unit>().invoke(kotlin.Result.success(Unit))
        }
    }

    @Before
    fun before() {
        Exponea.telemetry = null
        ExponeaContextProvider.applicationIsForeground = true
        fetchManager = mockk()
        messagesCache = mockk()
        var messageCacheTimestamp = 0L
        every { messagesCache.set(any()) } answers {
            messageCacheTimestamp = System.currentTimeMillis()
            every { messagesCache.getTimestamp() } returns messageCacheTimestamp
        }
        every { messagesCache.getTimestamp() } returns messageCacheTimestamp
        every { messagesCache.get() } returns arrayListOf()
        drawableCache = mockk()
        every { drawableCache.has(any()) } returns false
        every { drawableCache.preload(any(), any()) } answers {
            lastArg<((Boolean) -> Unit)?>()?.invoke(true)
        }
        every { drawableCache.clear() } just Runs
        fontCache = mockk()
        every { fontCache.has(any()) } returns false
        every { fontCache.preload(any(), any()) } answers {
            lastArg<((Boolean) -> Unit)?>()?.invoke(true)
        }
        every { fontCache.clear() } just Runs
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds(cookie = "mock-cookie")
        inAppMessageDisplayStateRepository = mockk()
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(null, null)
        every { inAppMessageDisplayStateRepository.setDisplayed(any(), any()) } just Runs
        every { inAppMessageDisplayStateRepository.setInteracted(any(), any()) } just Runs
        presenter = mockk()
        every { presenter.show(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        every { presenter.isPresenting() } returns false
        every { presenter.context } returns ApplicationProvider.getApplicationContext()
        trackingConsentManager = mockk()
        every { trackingConsentManager.trackInAppMessageError(any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageClose(any(), any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageClick(any(), any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppMessageShown(any(), any()) } just Runs
        val configuration = ExponeaConfiguration(projectToken = "mock-token")
        projectFactory = ExponeaProjectFactory(ApplicationProvider.getApplicationContext(), configuration)
        manager = InAppMessageManagerImpl(
            customerIdsRepository,
            messagesCache,
            fetchManager,
            inAppMessageDisplayStateRepository,
            drawableCache,
            fontCache,
            presenter,
            trackingConsentManager,
            projectFactory
        )
        mockActivity = Robolectric.buildActivity(Activity::class.java, Intent()).get()
        Exponea.inAppMessageActionCallback = Constants.InApps.defaultInAppMessageDelegate
    }

    @After
    fun after() {
        Exponea.telemetry = null
        Exponea.isStopped = false
    }

    @Test
    fun `should gracefully fail to preload with fetch error`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            lastArg<(Result<FetchError>) -> Unit>().invoke(Result(false, FetchError(null, "error")))
        }
        waitForIt {
            manager.reload { result ->
                it.assertTrue(result.isFailure)
                verify(exactly = 0) { messagesCache.set(any()) }
                it()
            }
        }
    }

    @Test
    fun `should preload messages`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle()))
            )
        }
        every { drawableCache.preload(any(), any()) } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(true)
        }

        waitForIt {
            manager.reload { result ->
                it.assertTrue(result.isSuccess)
                verify(exactly = 1) {
                    messagesCache.set(arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle()))
                }
                it()
            }
        }
    }

    @Test
    fun `should not preload messages if SDK is stopped`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle()))
            )
        }
        every { drawableCache.preload(any(), any()) } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(true)
        }
        waitForIt {
            Exponea.isStopped = true
            manager.reload { result ->
                it.assertFalse(result.isSuccess)
                verify(exactly = 0) {
                    messagesCache.set(arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle()))
                }
                it()
            }
        }
    }

    @Test
    fun `should ignore preloaded messages if SDK is stopped`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            Exponea.isStopped = true
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle()))
            )
        }
        every { drawableCache.preload(any(), any()) } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(true)
        }
        waitForIt {
            manager.reload { result ->
                it.assertFalse(result.isSuccess)
                verify(exactly = 0) {
                    messagesCache.set(arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle()))
                }
                it()
            }
        }
    }

    @Test
    fun `should always preload messages on first event`() {
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        messagesCache.set(arrayListOf())

        eventManager.track("test-event", Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    @Test
    fun `should not preload messages on first event while in background`() {
        ExponeaContextProvider.applicationIsForeground = false
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        messagesCache.set(arrayListOf())

        eventManager.track("test-event", Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
        verify(exactly = 0) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    @Test
    fun `should not preload messages for identifyCustomer while in background`() {
        ExponeaContextProvider.applicationIsForeground = false
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        messagesCache.set(arrayListOf())

        eventManager.track(
            type = EventType.TRACK_CUSTOMER
        )
        verify(exactly = 0) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    @Test
    fun `should not preload messages on push events and session-end`() {
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        messagesCache.set(arrayListOf())

        eventManager.track("delivered", currentTimeSeconds(), hashMapOf("prop" to "value"), EventType.PUSH_DELIVERED)
        eventManager.track("click", currentTimeSeconds(), hashMapOf("prop" to "value"), EventType.PUSH_OPENED)
        eventManager.track("sessionEnd", currentTimeSeconds(), hashMapOf("prop" to "value"), EventType.SESSION_END)
        verify(exactly = 0) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    private fun getEventManager(): EventManager {
        every { customerIdsRepository.get() } returns CustomerIds(cookie = "mock-cookie")
        val eventRepo = mockk<EventRepository>()
        every { eventRepo.add(any()) } just Runs
        val flushManager = mockk<FlushManager>()
        every { flushManager.flushData(any()) } just Runs
        val eventManager = EventManagerImpl(
            ExponeaConfiguration(projectToken = "mock-project-token"),
            eventRepo,
            customerIdsRepository,
            flushManager,
            projectFactory,
            onEventCreated = { event, type ->
                manager.onEventCreated(event, type)
            }
        )
        return eventManager
    }

    @Test
    fun `should preload only once when tracking from more threads`() {
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }

        val numberOfThreads = 5
        val latch = CountDownLatch(numberOfThreads)

        for (i in 0 until numberOfThreads) {
            runOnBackgroundThread {
                eventManager.track(
                    "test-event",
                    currentTimeSeconds(),
                    hashMapOf("prop" to "value"),
                    EventType.TRACK_EVENT
                )
                latch.countDown()
            }
        }
        latch.await(20, TimeUnit.SECONDS)
        verify(exactly = 1) {
            fetchManager.fetchInAppMessages(any(), any(), any(), any())
        }
    }

    @Test
    fun `should always track trigger events regardless on in app message preload state`() {
        Exponea.flushMode = FlushMode.MANUAL
        val customerIdsRepo = mockk<CustomerIdsRepository>()
        every { customerIdsRepo.get() } returns CustomerIds(cookie = "mock-cookie")
        val eventRepo = mockk<EventRepository>()
        val flushManager = mockk<FlushManager>()
        every { flushManager.flushData(any()) } just Runs
        val eventManager = EventManagerImpl(
                ExponeaConfiguration(projectToken = "mock-project-token"),
                eventRepo,
                customerIdsRepo,
                flushManager,
                projectFactory,
                onEventCreated = { event, type ->
                    manager.onEventCreated(event, type)
                }
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }

        val addedEvents: java.util.ArrayList<ExportedEvent> = arrayListOf()
        every { eventRepo.add(capture(addedEvents)) } just Runs

        val numberOfThreads = 5
        val latch = CountDownLatch(numberOfThreads)

        for (i in 0 until numberOfThreads) {
            runOnBackgroundThread {
                eventManager.track("test-event-$i", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
                latch.countDown()
            }
        }
        latch.await(20, TimeUnit.SECONDS)
        assertEquals(
            numberOfThreads,
            addedEvents.size
        )
    }
    @Test
    fun `should preload messages only once`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        manager.inAppShowingTriggered(
            EventType.SESSION_START,
            "session_start",
            hashMapOf(),
            currentTimeSeconds(),
            customerIdsRepository.get().toHashMap()
        )
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        manager.inAppShowingTriggered(
            EventType.SESSION_START,
            "session_start",
            hashMapOf(),
            currentTimeSeconds(),
            customerIdsRepository.get().toHashMap()
        )
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        manager.inAppShowingTriggered(
            EventType.SESSION_START,
            "session_start",
            hashMapOf(),
            currentTimeSeconds(),
            customerIdsRepository.get().toHashMap()
        )
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    @Test
    fun `should refresh messages only after expiration`() {
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { messagesCache.get() } returns arrayListOf()

        eventManager.track("test-event", currentTimeSeconds(), hashMapOf("prop" to "value"), EventType.SESSION_START)
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        eventManager.track("test-event", currentTimeSeconds(), hashMapOf("prop" to "value"), EventType.SESSION_START)
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        val expiredTimestamp = (Date().time + InAppMessageManagerImpl.REFRESH_CACHE_AFTER * 2).toDouble()
        eventManager.track("test-event", expiredTimestamp, hashMapOf("prop" to "value"), EventType.SESSION_START)
        verify(exactly = 2) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    @Test
    fun `should get null if no messages available`() {
        every { messagesCache.get() } returns arrayListOf()
        assertNull(manager.findMessagesByFilter("testEvent", hashMapOf(), null).firstOrNull())
    }

    @Test
    fun `should get message if both message and bitmap available`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(),
            InAppMessageTest.buildInAppMessageWithRichstyle()
        )
        every { drawableCache.has(any()) } returns true
        assertEquals(
            InAppMessageTest.buildInAppMessageWithRichstyle(),
            manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull()
        )
    }

    @Test
    fun `should get message if bitmap is blank`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(imageUrl = "")
        )
        assertEquals(
            InAppMessageTest.buildInAppMessageWithRichstyle(imageUrl = ""),
            manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull()
        )
    }

    @Test
    fun `should get message if fonts are blank`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(
                titleFontUrl = "",
                bodyFontUrl = "",
                buttonFontUrl = ""
            )
        )
        assertEquals(
            InAppMessageTest.buildInAppMessageWithRichstyle(
                titleFontUrl = "",
                bodyFontUrl = "",
                buttonFontUrl = ""
            ),
            manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull()
        )
    }

    @Test
    fun `should apply date filter`() {
        every { drawableCache.has(any()) } returns true
        val setupStoredEvent = { dateFilter: DateFilter ->
            every {
                messagesCache.get()
            } returns arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle(dateFilter = dateFilter))
        }

        val currentTime = (System.currentTimeMillis() / 1000).toInt()

        setupStoredEvent(DateFilter(true, null, null))
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(DateFilter(true, null, currentTime - 100))
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(DateFilter(true, null, currentTime + 100))
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(DateFilter(true, currentTime + 100, null))
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(DateFilter(true, currentTime - 100, null))
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(DateFilter(true, currentTime - 100, currentTime + 100))
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(DateFilter(true, currentTime + 100, currentTime + 100))
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(DateFilter(false, currentTime + 100, currentTime + 100))
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
    }

    @Test
    fun `should apply event filter`() {
        every { drawableCache.has(any()) } returns true
        val setupStoredEvent = { trigger: EventFilter ->
            every { messagesCache.get() } returns arrayListOf(
                InAppMessageTest.buildInAppMessageWithRichstyle(trigger = trigger)
            )
        }

        setupStoredEvent(EventFilter(eventType = "", filter = arrayListOf()))
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(EventFilter(eventType = "session_start", filter = arrayListOf()))
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(EventFilter(eventType = "payment", filter = arrayListOf()))
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())

        setupStoredEvent(
            EventFilter(
                eventType = "payment",
                filter = arrayListOf(
                    EventPropertyFilter.property("property", StringConstraint.startsWith("val"))
                )
            )
        )
        assertNull(manager.findMessagesByFilter("payment", hashMapOf(), null).firstOrNull())
        assertNull(manager.findMessagesByFilter("payment", hashMapOf("property" to "something"), null).firstOrNull())
        assertNotNull(manager.findMessagesByFilter("payment", hashMapOf("property" to "value"), null).firstOrNull())
    }

    @Test
    fun `should filter by priority`() {
        every { drawableCache.has(any()) } returns true
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "1"),
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "2"),
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "3")
        )
        assertEquals(
            manager.findMessagesByFilter("session_start", hashMapOf(), null),
            arrayListOf(
                InAppMessageTest.buildInAppMessageWithRichstyle(id = "1"),
                InAppMessageTest.buildInAppMessageWithRichstyle(id = "2"),
                InAppMessageTest.buildInAppMessageWithRichstyle(id = "3")
            )
        )
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "1", priority = 0),
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "2"),
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "3", priority = -1)
        )
        assertEquals(
            arrayListOf(
                InAppMessageTest.buildInAppMessageWithRichstyle(id = "1", priority = 0),
                InAppMessageTest.buildInAppMessageWithRichstyle(id = "2")
            ),
            manager.findMessagesByFilter("session_start", hashMapOf(), null)
        )
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "1", priority = 2),
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "2", priority = 2),
            InAppMessageTest.buildInAppMessageWithRichstyle(id = "3", priority = 1)
        )
        assertEquals(
            manager.findMessagesByFilter("session_start", hashMapOf(), null),
            arrayListOf(
                InAppMessageTest.buildInAppMessageWithRichstyle(id = "1", priority = 2),
                InAppMessageTest.buildInAppMessageWithRichstyle(id = "2", priority = 2)
            )
        )
    }

    @Test
    fun `should apply 'always' frequency filter`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(frequency = InAppMessageFrequency.ALWAYS.value)
        )
        every { drawableCache.has(any()) } returns true

        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
    }

    @Test
    fun `should apply 'only_once' frequency filter`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(frequency = InAppMessageFrequency.ONLY_ONCE.value)
        )
        every { drawableCache.has(any()) } returns true
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1000), null)
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
    }

    @Test
    fun `should apply 'until_visitor_interacts' frequency filter`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(
                frequency = InAppMessageFrequency.UNTIL_VISITOR_INTERACTS.value
            )
        )
        every { drawableCache.has(any()) } returns true
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1000), null)
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1000), Date(1000))
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
    }

    @Test
    fun `should apply 'once_per_visit' frequency filter`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle(frequency = InAppMessageFrequency.ONCE_PER_VISIT.value)
        )
        every { drawableCache.has(any()) } returns true
        manager.sessionStarted(Date(1000))
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1500), null)
        assertNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
        manager.sessionStarted(Date(2000))
        assertNotNull(manager.findMessagesByFilter("session_start", hashMapOf(), null).firstOrNull())
    }

    @Test
    fun `should set message displayed and interacted`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle())
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<(Activity, Boolean, InAppMessagePayloadButton?) -> Unit>()
        val errorCallbackSlot = slot<(String) -> Unit>()
        every {
            presenter.show(
                any(), any(), any(), any(), any(),
                capture(actionCallbackSlot), capture(dismissedCallbackSlot),
                capture(errorCallbackSlot)
            )
        } returns mockk()

        waitForIt { manager.reload { it() } }
        runBlocking {
            manager.inAppShowingTriggered(
                EventType.SESSION_START,
                "session_start",
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }
        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) { inAppMessageDisplayStateRepository.setDisplayed(any(), any()) }
        actionCallbackSlot.captured.invoke(
            mockActivity,
            InAppMessageTest.buildInAppMessageWithRichstyle().payload!!.buttons!![0]
        )
        verify(exactly = 1) { inAppMessageDisplayStateRepository.setInteracted(any(), any()) }
    }

    @Test
    fun `should track dialog lifecycle`() {
        val inAppMessage = InAppMessageTest.buildInAppMessageWithRichstyle()
        every { messagesCache.get() } returns arrayListOf(inAppMessage)
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>()
                .invoke(Result(true, arrayListOf(inAppMessage)))
        }
        val showInvoked = CountDownLatch(1)
        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<(Activity, Boolean, InAppMessagePayloadButton?) -> Unit>()
        val errorCallbackSlot = slot<(String) -> Unit>()
        val spykManager = spyk(manager)
        every {
            presenter.show(
                any(), any(), any(), any(), any(),
                capture(actionCallbackSlot), capture(dismissedCallbackSlot), capture(errorCallbackSlot)
            )
        } answers {
            showInvoked.countDown()
            mockk()
        }
        waitForIt { spykManager.reload { it() } }
        registerPendingRequest("session_start", spykManager)
        spykManager.pickAndShowMessage()
        assertTrue(showInvoked.await(2, TimeUnit.SECONDS))
        Robolectric.flushForegroundThreadScheduler()
        val button = inAppMessage.payload!!.buttons!![0]
        val cancelButton = inAppMessage.payload!!.buttons!![1]
        actionCallbackSlot.captured.invoke(mockActivity, button)
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageClick(
                inAppMessage,
                "Action",
                "https://someaddress.com",
                CONSIDER_CONSENT
            )
        }
        verify(exactly = 1) {
            spykManager.processInAppMessageAction(mockActivity, button)
        }
        // dismiss by non-interaction (system)
        dismissedCallbackSlot.captured.invoke(mockActivity, false, null)
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageClose(
                inAppMessage, null, false, CONSIDER_CONSENT
            )
        }
        // dismiss by interaction with default close button/gesture
        dismissedCallbackSlot.captured.invoke(mockActivity, true, null)
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageClose(
                inAppMessage, null, true, CONSIDER_CONSENT
            )
        }
        // dismiss by interaction with cancel button
        dismissedCallbackSlot.captured.invoke(mockActivity, true, cancelButton)
        assertEquals("Cancel", cancelButton.text)
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageClose(
                inAppMessage, "Cancel", true, CONSIDER_CONSENT
            )
        }
    }

    @Test
    fun `should open deeplink once button is clicked`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle())
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val errorCallbackSlot = slot<(String) -> Unit>()
        every { presenter.show(
            any(), any(), any(), any(), any(),
            capture(actionCallbackSlot), any(), capture(errorCallbackSlot)
        ) } returns mockk()
        waitForIt { manager.reload { it() } }
        runBlocking {
            manager.inAppShowingTriggered(
                EventType.SESSION_START,
                "session_start",
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }
        Robolectric.flushForegroundThreadScheduler()

        val button = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!.buttons!![0]
        assertNull(shadowOf(mockActivity).nextStartedActivityForResult)
        assertTrue(actionCallbackSlot.isCaptured)
        actionCallbackSlot.captured.invoke(mockActivity, button)
        assertEquals(
            button.link,
            shadowOf(mockActivity).nextStartedActivityForResult.intent.data?.toString()
        )
    }

    @Test
    fun `delegate should create event data`() {
        val eventManager = mockk<EventManager>()
        val eventTypeSlot = slot<String>()
        val propertiesSlot = slot<HashMap<String, Any>>()
        val typeSlot = slot<EventType>()
        every {
            eventManager.processTrack(capture(eventTypeSlot), any(), capture(propertiesSlot), capture(typeSlot), any())
        } just Runs

        val delegate = EventManagerInAppMessageTrackingDelegate(
            ApplicationProvider.getApplicationContext(),
            eventManager
        )
        delegate.track(InAppMessageTest.buildInAppMessageWithRichstyle(), "mock-action", false, true)
        assertEquals(Constants.EventTypes.banner, eventTypeSlot.captured)
        assertEquals(EventType.BANNER, typeSlot.captured)
        assertEquals("5dd86f44511946ea55132f29", propertiesSlot.captured["banner_id"])

        assertEquals("Test serving in-app message", propertiesSlot.captured["banner_name"])
        assertEquals("mock-action", propertiesSlot.captured["action"])
        assertEquals(false, propertiesSlot.captured["interaction"])
        assertEquals(0, propertiesSlot.captured["variant_id"])
        assertEquals("Variant A", propertiesSlot.captured["variant_name"])
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should preload pending message image first and show it`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessage = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url"
        )
        val otherMessage1 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter("other_event", arrayListOf()),
            imageUrl = "other_image_url_1"
        )
        val otherMessage2 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter("other_event", arrayListOf()),
            imageUrl = "other_image_url_2"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage, otherMessage1, otherMessage2))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage, otherMessage1, otherMessage2)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null

        manager.inAppShowingTriggered(
            EventType.SESSION_END,
            activeEventType,
            hashMapOf(),
            currentTimeSeconds(),
            customerIds
        )

        waitForIt { manager.reload { _ -> it() } }

        verifySequence {
            presenter.isPresenting()
            messagesCache.get()
            messagesCache.get()
            drawableCache.preload(arrayListOf("pending_image_url"), any())
            drawableCache.preload(arrayListOf(), any())
            drawableCache.getDrawable(any<String>())
            drawableCache.getDrawable(any<String>())
            presenter.show(InAppMessageType.MODAL, pendingMessage.payload, any(), any(), any(), any(), any(), any())
            presenter.context
            messagesCache.set(arrayListOf(pendingMessage, otherMessage1, otherMessage2))
            drawableCache.preload(arrayListOf("pending_image_url", "other_image_url_1", "other_image_url_2"), any())
        }
        confirmVerified(messagesCache, drawableCache, presenter)
    }

    @Test
    fun `should delay in-app message presenting`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        val message = InAppMessageTest.buildInAppMessageWithRichstyle(delay = 1234)
        every { messagesCache.get() } returns arrayListOf(message)
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { presenter.show(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk()

        waitForIt { manager.reload { it() } }
        runBlocking {
            manager.inAppShowingTriggered(
                EventType.SESSION_START,
                "session_start",
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }
        Robolectric.getForegroundThreadScheduler().advanceBy(1233, TimeUnit.MILLISECONDS)
        verify(exactly = 0) {
            presenter.show(InAppMessageType.MODAL, message.payload, any(), any(), any(), any(), any(), any())
        }
        Robolectric.getForegroundThreadScheduler().advanceBy(1, TimeUnit.MILLISECONDS)
        verify(exactly = 1) {
            presenter.show(InAppMessageType.MODAL, message.payload, any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `should pass timeout to in-app message presenter`() {
        val customerIds = customerIdsRepository.get().toHashMap()
        val message = InAppMessageTest.buildInAppMessageWithRichstyle(timeout = 1234)
        every { messagesCache.get() } returns arrayListOf(message)
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val showCalled = CountDownLatch(1)
        every {
            presenter.show(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            showCalled.countDown()
            mockk()
        }

        waitForIt { manager.reload { it() } }
        manager.inAppShowingTriggered(
            EventType.SESSION_START,
            "session_start",
            hashMapOf(),
            currentTimeSeconds(),
            customerIds
        )
        assertTrue(showCalled.await(10, TimeUnit.SECONDS))
        verify {
            presenter.show(InAppMessageType.MODAL, message.payload, any(), any(), 1234, any(), any(), any())
        }
    }

    private fun registerPendingRequest(
        eventType: String,
        managerInstance: InAppMessageManagerImpl = manager,
        customerIds: HashMap<String, String?> = customerIdsRepository.get().toHashMap()
    ) {
        managerInstance.pendingShowRequests = arrayListOf(
            InAppMessageShowRequest(
                eventType,
                hashMapOf(),
                currentTimeSeconds(),
                System.currentTimeMillis(),
                customerIds
            )
        )
    }

    @Test
    fun `should track control group message without showing it`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        val message = InAppMessageTest.getInAppMessageForControlGroup(
            variantId = -1,
            variantName = "Control group",
            timeout = 1234
        )
        every { messagesCache.get() } returns arrayListOf(message)
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { presenter.show(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        waitForIt { manager.reload { it() } }
        runBlocking {
            manager.inAppShowingTriggered(
                EventType.SESSION_START,
                "session_start",
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) { trackingConsentManager.trackInAppMessageShown(message, CONSIDER_CONSENT) }
        verify(exactly = 0) { presenter.show(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track and perform all actions if its enabled in callback`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle())
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val spykCallback: InAppMessageCallback = spyk(object : InAppMessageCallback {
            override var overrideDefaultBehavior = false
            override var trackActions = true
            override fun inAppMessageShown(message: InAppMessage, context: Context) {}
            override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {}
            override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {}
            override fun inAppMessageCloseAction(
                message: InAppMessage,
                button: InAppMessageButton?,
                interaction: Boolean,
                context: Context
            ) {}
        })
        Exponea.inAppMessageActionCallback = spykCallback

        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<(Activity, Boolean, InAppMessagePayloadButton?) -> Unit>()
        val errorCallbackSlot = slot<(String) -> Unit>()
        val spykManager = spyk(manager)

        every {
            presenter.show(
                any(), any(), any(), any(), any(),
                capture(actionCallbackSlot), capture(dismissedCallbackSlot), capture(errorCallbackSlot)
            )
        } returns mockk()

        waitForIt { spykManager.reload { it() } }
        runBlocking {
            spykManager.inAppShowingTriggered(
                EventType.SESSION_START,
                "session_start",
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }

        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageShown(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                CONSIDER_CONSENT
            )
        }
        val button = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!.buttons!![0]
        actionCallbackSlot.captured.invoke(mockActivity, button)
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageClick(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                "Action",
                "https://someaddress.com",
                CONSIDER_CONSENT
            )
        }
        verify(exactly = 1) {
            spykCallback.inAppMessageClickAction(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                InAppMessageButton(button.text, button.link),
                mockActivity
            )
        }
        verify(exactly = 1) {
            spykManager.processInAppMessageAction(mockActivity, button)
        }
        dismissedCallbackSlot.captured.invoke(mockActivity, false, null)
        verify(exactly = 1) {
            spykCallback.inAppMessageCloseAction(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                null,
                false,
                mockActivity
            )
        }
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageClose(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                null,
                false,
                CONSIDER_CONSENT
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should not track and perform any actions if its disabled in callback`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle())
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val spykCallback: InAppMessageCallback = spyk(object : InAppMessageCallback {
            override var overrideDefaultBehavior = true
            override var trackActions = false
            override fun inAppMessageShown(message: InAppMessage, context: Context) {}
            override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {}
            override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {}
            override fun inAppMessageCloseAction(
                message: InAppMessage,
                button: InAppMessageButton?,
                interaction: Boolean,
                context: Context
            ) {}
        })
        Exponea.inAppMessageActionCallback = spykCallback

        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<(Activity, Boolean, InAppMessagePayloadButton?) -> Unit>()
        val errorCallbackSlot = slot<(String) -> Unit>()
        val spykManager = spyk(manager)

        every {
            presenter.show(
                any(), any(), any(), any(), any(),
                capture(actionCallbackSlot), capture(dismissedCallbackSlot), capture(errorCallbackSlot)
            )
        } returns mockk()

        waitForIt { spykManager.reload { it() } }
        runBlocking {
            spykManager.inAppShowingTriggered(
                EventType.SESSION_START,
                "session_start",
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }

        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageShown(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                CONSIDER_CONSENT
            )
        }
        val button = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!.buttons!![0]
        actionCallbackSlot.captured.invoke(mockActivity, button)
        verify(exactly = 0) {
            trackingConsentManager.trackInAppMessageClick(any(), any(), any(), any())
        }
        verify(exactly = 1) {
            spykCallback.inAppMessageClickAction(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                InAppMessageButton(button.text, button.link),
                mockActivity
            )
        }
        verify(exactly = 0) {
            spykManager.processInAppMessageAction(any(), any())
        }
        dismissedCallbackSlot.captured.invoke(mockActivity, false, null)
        verify(exactly = 1) {
            spykCallback.inAppMessageCloseAction(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                null,
                false,
                mockActivity
            )
        }
        verify(exactly = 0) {
            trackingConsentManager.trackInAppMessageClose(any(), any(), any(), any())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track action when track is called in callback`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle())
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val spykCallback: InAppMessageCallback = spyk(object : InAppMessageCallback {
            override var overrideDefaultBehavior = true
            override var trackActions = false
            override fun inAppMessageShown(message: InAppMessage, context: Context) {}
            override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {}
            override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {
                trackingConsentManager.trackInAppMessageClick(message, button.text, button.url, CONSIDER_CONSENT)
            }
            override fun inAppMessageCloseAction(
                message: InAppMessage,
                button: InAppMessageButton?,
                interaction: Boolean,
                context: Context
            ) {
                trackingConsentManager.trackInAppMessageClose(message, button?.text, interaction, CONSIDER_CONSENT)
            }
        })
        Exponea.inAppMessageActionCallback = spykCallback

        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<(Activity, Boolean, InAppMessagePayloadButton?) -> Unit>()
        val errorCallbackSlot = slot<(String) -> Unit>()
        val spykManager = spyk(manager)

        every {
            presenter.show(
                any(), any(), any(), any(), any(),
                capture(actionCallbackSlot), capture(dismissedCallbackSlot), capture(errorCallbackSlot)
            )
        } returns mockk()

        waitForIt { spykManager.reload { it() } }
        runBlocking {
            spykManager.inAppShowingTriggered(
                EventType.SESSION_START,
                "session_start",
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }

        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageShown(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                CONSIDER_CONSENT
            )
        }
        val button = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!.buttons!![0]
        actionCallbackSlot.captured.invoke(mockActivity, button)
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageClick(
                InAppMessageTest.buildInAppMessageWithRichstyle(),
                "Action",
                "https://someaddress.com",
                CONSIDER_CONSENT
            )
        }
    }

    @Test
    fun `should clear pendingShowRequests on identifyCustomer for MANUAL flushMode`() {
        val customerIds = customerIdsRepository.get().toHashMap()
        Exponea.flushMode = FlushMode.MANUAL
        // track event for future to prevent messages reload
        val distantFutureSeconds = (System.currentTimeMillis() / 1000.0) + TimeUnit.HOURS.toSeconds(1)
        manager.pendingShowRequests += InAppMessageShowRequest(
            "mock-type",
            mapOf("mock-prop" to "mock-val"),
            distantFutureSeconds,
            System.currentTimeMillis(),
            customerIds
        )
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        messagesCache.set(arrayListOf())
        // use null eventType to skip message showing
        eventManager.track(null, Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.TRACK_CUSTOMER)
        assertEquals(0, manager.pendingShowRequests.size)
    }

    @Test
    fun `should clear pendingShowRequests on identifyCustomer for IMMEDIATE flushMode`() {
        val customerIds = customerIdsRepository.get().toHashMap()
        Exponea.flushMode = FlushMode.IMMEDIATE
        // track event for future to prevent messages reload
        val distantFutureSeconds = (System.currentTimeMillis() / 1000.0) + TimeUnit.HOURS.toSeconds(1)
        manager.pendingShowRequests += InAppMessageShowRequest(
            "mock-type",
            mapOf("mock-prop" to "mock-val"),
            distantFutureSeconds,
            System.currentTimeMillis(),
            customerIds
        )
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        messagesCache.set(arrayListOf())
        // use null eventType to skip message showing
        eventManager.track(null, Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.TRACK_CUSTOMER)
        assertTrue(manager.pendingShowRequests.isEmpty())
    }

    @Test
    fun `should not clear pendingShowRequests on track event for MANUAL flushMode`() {
        val customerIds = customerIdsRepository.get().toHashMap()
        Exponea.flushMode = FlushMode.MANUAL
        // track event for future to prevent messages reload
        val distantFutureSeconds = (System.currentTimeMillis() / 1000.0) + TimeUnit.HOURS.toSeconds(1)
        manager.pendingShowRequests += InAppMessageShowRequest(
            "mock-type",
            mapOf("mock-prop" to "mock-val"),
            distantFutureSeconds,
            System.currentTimeMillis(),
            customerIds
        )
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        // simulate loading, to invoke STOP ReloadMode
        manager.preloadStarted()
        messagesCache.set(arrayListOf())
        // trigger
        eventManager.track(null, Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.SESSION_START)
        assertEquals(1, manager.pendingShowRequests.size)
    }

    @Test
    fun `should not clear pendingShowRequests on track event for IMMEDIATE flushMode`() {
        val customerIds = customerIdsRepository.get().toHashMap()
        Exponea.flushMode = FlushMode.IMMEDIATE
        // track event for future to prevent messages reload
        val distantFutureSeconds = (System.currentTimeMillis() / 1000.0) + TimeUnit.HOURS.toSeconds(1)
        manager.pendingShowRequests += InAppMessageShowRequest(
            "mock-type",
            mapOf("mock-prop" to "mock-val"),
            distantFutureSeconds,
            System.currentTimeMillis(),
            customerIds
        )
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        // simulate loading, to invoke STOP ReloadMode
        manager.preloadStarted()
        messagesCache.set(arrayListOf())
        // trigger
        eventManager.track(null, Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.SESSION_START)
        assertEquals(1, manager.pendingShowRequests.size)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should register pending request with trigger immediately`() = runInSingleThread { idleThreads ->
        val customerIds = customerIdsRepository.get().toHashMap()
        val activeEventType = Constants.EventTypes.sessionStart
        val pendingMessage = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url"
        )
        val otherMessage1 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter("other_event", arrayListOf()),
            imageUrl = "other_image_url_1"
        )
        val otherMessage2 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter("other_event", arrayListOf()),
            imageUrl = "other_image_url_2"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage, otherMessage1, otherMessage2))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage, otherMessage1, otherMessage2)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null

        // simulate loading, to invoke STOP ReloadMode
        manager.preloadStarted()
        messagesCache.set(arrayListOf(pendingMessage, otherMessage1, otherMessage2))
        // trigger
        manager.inAppShowingTriggered(
            EventType.SESSION_START,
            activeEventType,
            hashMapOf(),
            currentTimeSeconds(),
            customerIds
        )
        assertEquals(1, manager.pendingShowRequests.size)
        assertNotNull(manager.pendingShowRequests.firstOrNull())
        assertEquals(activeEventType, manager.pendingShowRequests.first().eventType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should register single pending request for multiple same triggers`() {
        val customerIds = customerIdsRepository.get().toHashMap()
        val activeEventType = Constants.EventTypes.sessionEnd
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf())
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf()
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null

        for (i in 0..10) {
            manager.registerPendingShowRequest(
                activeEventType,
                hashMapOf(),
                currentTimeSeconds(),
                customerIds
            )
        }
        assertEquals(1, manager.pendingShowRequests.size)
        assertNotNull(manager.pendingShowRequests.firstOrNull())
        assertEquals(activeEventType, manager.pendingShowRequests.first().eventType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should register pending request by latest event timestamp`() {
        val customerIds = customerIdsRepository.get().toHashMap()
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessage = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null

        val latestEventTimestamp = currentTimeSeconds()
        for (i in 0..10) {
            manager.registerPendingShowRequest(
                activeEventType,
                hashMapOf(),
                latestEventTimestamp - (i * 1000),
                customerIds
            )
        }
        assertEquals(1, manager.pendingShowRequests.size)
        assertNotNull(manager.pendingShowRequests.firstOrNull())
        assertEquals(activeEventType, manager.pendingShowRequests.first().eventType)
        assertEquals(latestEventTimestamp, manager.pendingShowRequests.first().eventTimestamp)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should pick no message if another is shown`() {
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessage = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        registerPendingRequest(activeEventType)
        every { presenter.isPresenting() } returns true
        val pickedMessage = manager.pickPendingMessage()
        assertNull(pickedMessage)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should pick no message if customer IDs not matched`() {
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessage = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        val customerIds = hashMapOf<String, String?>(
            "cookie" to "123456",
            "registed" to "john@doe.com"
        )
        registerPendingRequest(activeEventType, customerIds = customerIds)
        every { presenter.isPresenting() } returns false
        val pickedMessage = manager.pickPendingMessage()
        assertNull(pickedMessage)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should pick no message if filter not matched`() {
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessage = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter("not_active_event_type", arrayListOf()),
            imageUrl = "pending_image_url"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        registerPendingRequest(activeEventType)
        every { presenter.isPresenting() } returns false
        val pickedMessage = manager.pickPendingMessage()
        assertNull(pickedMessage)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should pick message with top priority`() {
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessageTopPrio = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = 100,
            id = "12345"
        )
        val pendingMessageSecond = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = 10,
            id = "67890"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessageTopPrio, pendingMessageSecond))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessageTopPrio, pendingMessageSecond)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        registerPendingRequest(activeEventType)
        every { presenter.isPresenting() } returns false
        val pickedMessage = manager.pickPendingMessage()
        assertNotNull(pickedMessage)
        assertEquals(pendingMessageTopPrio.id, pickedMessage.second.id)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should pick random message with same priority`() {
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessage1 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = 100,
            id = "12345"
        )
        val pendingMessage2 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = 100,
            id = "67890"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage1, pendingMessage2))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage1, pendingMessage2)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        registerPendingRequest(activeEventType)
        every { presenter.isPresenting() } returns false
        val pickedMessage = manager.pickPendingMessage()
        assertNotNull(pickedMessage)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should pick random message with null priority`() {
        val activeEventType = Constants.EventTypes.sessionEnd
        val pendingMessage1 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "12345"
        )
        val pendingMessage2 = InAppMessageTest.buildInAppMessageWithRichstyle(
            trigger = EventFilter(activeEventType, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "67890"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage1, pendingMessage2))
            )
        }
        every { drawableCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage1, pendingMessage2)
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        registerPendingRequest(activeEventType)
        every { presenter.isPresenting() } returns false
        val pickedMessage = manager.pickPendingMessage()
        assertNotNull(pickedMessage)
    }

    @Test
    fun `should track telemetry on message shown`() {
        mockkConstructorFix(TelemetryManager::class)
        val telemetryTelemetryEventSlot = slot<com.exponea.sdk.telemetry.model.TelemetryEvent>()
        val telemetryPropertiesSlot = slot<MutableMap<String, String>>()
        every {
            anyConstructed<TelemetryManager>().reportEvent(
                capture(telemetryTelemetryEventSlot),
                capture(telemetryPropertiesSlot)
            )
        } just Runs
        Exponea.telemetry = TelemetryManager(ApplicationProvider.getApplicationContext())
        val inAppMessage = InAppMessageTest.buildInAppMessageWithRichstyle()
        every { messagesCache.get() } returns arrayListOf(inAppMessage)
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>()
                .invoke(Result(true, arrayListOf(inAppMessage)))
        }
        val presentCalled = CountDownLatch(1)
        val spykManager = spyk(manager)
        every {
            presenter.show(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            presentCalled.countDown()
            mockk()
        }
        waitForIt { spykManager.reload { it() } }
        registerPendingRequest("session_start", spykManager)
        spykManager.pickAndShowMessage()
        assertTrue(presentCalled.await(10, TimeUnit.SECONDS))
        verify(exactly = 1) {
            trackingConsentManager.trackInAppMessageShown(inAppMessage, CONSIDER_CONSENT)
        }
        assertTrue(telemetryTelemetryEventSlot.isCaptured)
        val capturedEventType = telemetryTelemetryEventSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.IN_APP_MESSAGE_SHOWN, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertEquals("modal", capturedProps["type"])
    }

    @Test
    fun `should track telemetry on messages fetch`() {
        mockkConstructorFix(TelemetryManager::class)
        val telemetryTelemetryEventSlot = slot<com.exponea.sdk.telemetry.model.TelemetryEvent>()
        val telemetryPropertiesSlot = slot<MutableMap<String, String>>()
        every {
            anyConstructed<TelemetryManager>().reportEvent(
                capture(telemetryTelemetryEventSlot),
                capture(telemetryPropertiesSlot)
            )
        } just Runs
        Exponea.telemetry = TelemetryManager(ApplicationProvider.getApplicationContext())
        val inAppMessage = InAppMessageTest.buildInAppMessageWithRichstyle()
        every { messagesCache.get() } returns arrayListOf(inAppMessage)
        every { drawableCache.has(any()) } returns true
        every { drawableCache.getFile(any()) } returns MockFile()
        every { drawableCache.getDrawable(any<String>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { drawableCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { fontCache.has(any()) } returns true
        every { fontCache.getFontFile(any()) } returns MockFile()
        every { fontCache.getTypeface(any()) } returns null
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>()
                .invoke(Result(true, arrayListOf(inAppMessage)))
        }
        val presentCalled = CountDownLatch(1)
        val spykManager = spyk(manager)
        every {
            presenter.show(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            presentCalled.countDown()
            mockk()
        }
        waitForIt { spykManager.reload { it() } }
        assertTrue(telemetryTelemetryEventSlot.isCaptured)
        val capturedEventType = telemetryTelemetryEventSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.IN_APP_MESSAGE_FETCH, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertEquals("1", capturedProps["count"])
        assertTrue(capturedProps["data"]!!.isNotEmpty())
    }
}
