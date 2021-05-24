package com.exponea.sdk.manager

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DatabaseStorageObject
import com.exponea.sdk.models.DateFilter
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppMessage
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
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.testutil.waitForIt
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageManagerImplTest {
    private lateinit var fetchManager: FetchManager
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var inAppMessageDisplayStateRepository: InAppMessageDisplayStateRepository
    private lateinit var messagesCache: InAppMessagesCache
    private lateinit var bitmapCache: InAppMessageBitmapCache
    private lateinit var presenter: InAppMessagePresenter
    private lateinit var manager: InAppMessageManagerImpl
    private lateinit var mockActivity: Activity
    @Before
    fun before() {
        fetchManager = mockk()
        messagesCache = mockk()
        every { messagesCache.set(any()) } just Runs
        every { messagesCache.getTimestamp() } returns System.currentTimeMillis()
        bitmapCache = mockk()
        every { bitmapCache.has(any()) } returns false
        every { bitmapCache.preload(any(), any()) } just Runs
        every { bitmapCache.clearExcept(any()) } just Runs
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds()
        inAppMessageDisplayStateRepository = mockk()
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(null, null)
        every { inAppMessageDisplayStateRepository.setDisplayed(any(), any()) } just Runs
        every { inAppMessageDisplayStateRepository.setInteracted(any(), any()) } just Runs
        presenter = mockk()
        every { presenter.show(any(), any(), any(), any(), any(), any()) } returns mockk()
        manager = InAppMessageManagerImpl(
            ExponeaConfiguration(),
            customerIdsRepository,
            messagesCache,
            fetchManager,
            inAppMessageDisplayStateRepository,
            bitmapCache,
            presenter
        )
        mockActivity = Robolectric.buildActivity(Activity::class.java, Intent()).get()
    }

    @Test
    fun `should gracefully fail to preload with fetch error`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            lastArg<(Result<FetchError>) -> Unit>().invoke(Result(false, FetchError(null, "error")))
        }
        waitForIt {
            manager.preload { result ->
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
                Result(true, arrayListOf(InAppMessageTest.getInAppMessage()))
            )
        }
        every { bitmapCache.preload(any(), any()) } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(true)
        }

        waitForIt {
            manager.preload { result ->
                it.assertTrue(result.isSuccess)
                verify(exactly = 1) { messagesCache.set(arrayListOf(InAppMessageTest.getInAppMessage())) }
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

    private fun getEventManager(): EventManager {
        val customerIdsRepo = mockk<CustomerIdsRepository>()
        every { customerIdsRepo.get() } returns CustomerIds(cookie = "mock-cookie")
        val eventRepo = mockk<EventRepository>()
        every { eventRepo.add(any()) } returns true
        val flushManager = mockk<FlushManager>()
        every { flushManager.flushData(any()) } just Runs
        val eventManager = EventManagerImpl(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(projectToken = "mock-project-token"),
            eventRepo,
            customerIdsRepo,
            flushManager,
            manager
        )
        return eventManager
    }

    @Test
    fun `should preload only once when tracking from more threads`() {
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { messagesCache.get() } returns arrayListOf()
        messagesCache.set(arrayListOf())

        val numberOfThreads = 5
        val service: ExecutorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(numberOfThreads)

        for (i in 0 until numberOfThreads) {
            service.submit {
                eventManager.track("test-event", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
                latch.countDown()
            }
        }
        latch.await()
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        }

    @Test
    fun `should always track trigger events regardless on in app message preload state`() {
        val customerIdsRepo = mockk<CustomerIdsRepository>()
        every { customerIdsRepo.get() } returns CustomerIds(cookie = "mock-cookie")
        val eventRepo = mockk<EventRepository>()
        val flushManager = mockk<FlushManager>()
        every { flushManager.flushData(any()) } just Runs
        val eventManager = EventManagerImpl(
                ApplicationProvider.getApplicationContext(),
                ExponeaConfiguration(projectToken = "mock-project-token"),
                eventRepo,
                customerIdsRepo,
                flushManager,
                manager
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { messagesCache.get() } returns arrayListOf()

        var addedEvents: java.util.ArrayList<DatabaseStorageObject<ExportedEventType>> = arrayListOf()
        every { eventRepo.add(capture(addedEvents)) } returns true

        val numberOfThreads = 5
        val service: ExecutorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(numberOfThreads)

        for (i in 0 until numberOfThreads) {
            service.submit {
                eventManager.track("test-event-$i", 123.0, hashMapOf("prop" to "value"), EventType.TRACK_EVENT)
                latch.countDown()
            }
        }
        latch.await()
        assertEquals(
                addedEvents.size,
                numberOfThreads
        )
    }
    @Test
    fun `should preload messages only once`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        manager.preloadIfNeeded(Date().time.toDouble())
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        manager.preloadIfNeeded(Date().time.toDouble())
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        manager.preloadIfNeeded(Date().time.toDouble())
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    @Test
    fun `should refresh messages only after expiration`() {
        val eventManager = getEventManager()
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { messagesCache.get() } returns arrayListOf()

        eventManager.track("test-event", Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.SESSION_START)
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        eventManager.track("test-event", Date().time.toDouble(), hashMapOf("prop" to "value"), EventType.SESSION_START)
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        val expiredTimestamp = (Date().time + InAppMessageManagerImpl.REFRESH_CACHE_AFTER * 2).toDouble()
        eventManager.track("test-event", expiredTimestamp, hashMapOf("prop" to "value"), EventType.SESSION_START)
        verify(exactly = 2) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
    }

    @Test
    fun `should get null if no messages available`() {
        every { messagesCache.get() } returns arrayListOf()
        assertNull(manager.getRandom("testEvent", hashMapOf(), null))
    }

    @Test
    fun `should get message if both message and bitmap available`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(),
            InAppMessageTest.getInAppMessage()
        )
        every { bitmapCache.has(any()) } returns true
        assertEquals(InAppMessageTest.getInAppMessage(), manager.getRandom("session_start", hashMapOf(), null))
    }

    @Test
    fun `should not get message if bitmap is not available`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(),
            InAppMessageTest.getInAppMessage()
        )
        assertEquals(null, manager.getRandom("session_start", hashMapOf(), null))
    }

    @Test
    fun `should get message if bitmap is blank`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(imageUrl = ""),
            InAppMessageTest.getInAppMessage()
        )
        assertEquals(
            InAppMessageTest.getInAppMessage(imageUrl = ""),
            manager.getRandom("session_start", hashMapOf(), null)
        )
    }

    @Test
    fun `should apply date filter`() {
        every { bitmapCache.has(any()) } returns true
        val setupStoredEvent = { dateFilter: DateFilter ->
            every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage(dateFilter = dateFilter))
        }

        val currentTime = (System.currentTimeMillis() / 1000).toInt()

        setupStoredEvent(DateFilter(true, null, null))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(DateFilter(true, null, currentTime - 100))
        assertNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(DateFilter(true, null, currentTime + 100))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(DateFilter(true, currentTime + 100, null))
        assertNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(DateFilter(true, currentTime - 100, null))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(DateFilter(true, currentTime - 100, currentTime + 100))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(DateFilter(true, currentTime + 100, currentTime + 100))
        assertNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(DateFilter(false, currentTime + 100, currentTime + 100))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))
    }

    @Test
    fun `should apply event filter`() {
        every { bitmapCache.has(any()) } returns true
        val setupStoredEvent = { trigger: EventFilter ->
            every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage(trigger = trigger))
        }

        setupStoredEvent(EventFilter(eventType = "", filter = arrayListOf()))
        assertNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(EventFilter(eventType = "session_start", filter = arrayListOf()))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(EventFilter(eventType = "payment", filter = arrayListOf()))
        assertNull(manager.getRandom("session_start", hashMapOf(), null))

        setupStoredEvent(
            EventFilter(
                eventType = "payment",
                filter = arrayListOf(
                    EventPropertyFilter.property("property", StringConstraint.startsWith("val"))
                )
            )
        )
        assertNull(manager.getRandom("payment", hashMapOf(), null))
        assertNull(manager.getRandom("payment", hashMapOf("property" to "something"), null))
        assertNotNull(manager.getRandom("payment", hashMapOf("property" to "value"), null))
    }

    @Test
    fun `should filter by priority`() {
        every { bitmapCache.has(any()) } returns true
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(id = "1"),
            InAppMessageTest.getInAppMessage(id = "2"),
            InAppMessageTest.getInAppMessage(id = "3")
        )
        assertEquals(
            manager.getFilteredMessages("session_start", hashMapOf(), null),
            arrayListOf(
                InAppMessageTest.getInAppMessage(id = "1"),
                InAppMessageTest.getInAppMessage(id = "2"),
                InAppMessageTest.getInAppMessage(id = "3")
            )
        )
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(id = "1", priority = 0),
            InAppMessageTest.getInAppMessage(id = "2"),
            InAppMessageTest.getInAppMessage(id = "3", priority = -1)
        )
        assertEquals(
            manager.getFilteredMessages("session_start", hashMapOf(), null),
            arrayListOf(
                InAppMessageTest.getInAppMessage(id = "1", priority = 0),
                InAppMessageTest.getInAppMessage(id = "2")
            )
        )
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(id = "1", priority = 2),
            InAppMessageTest.getInAppMessage(id = "2", priority = 2),
            InAppMessageTest.getInAppMessage(id = "3", priority = 1)
        )
        assertEquals(
            manager.getFilteredMessages("session_start", hashMapOf(), null),
            arrayListOf(
                InAppMessageTest.getInAppMessage(id = "1", priority = 2),
                InAppMessageTest.getInAppMessage(id = "2", priority = 2)
            )
        )
    }

    @Test
    fun `should apply 'always' frequency filter`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(frequency = InAppMessageFrequency.ALWAYS.value)
        )
        every { bitmapCache.has(any()) } returns true

        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))
    }

    @Test
    fun `should apply 'only_once' frequency filter`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(frequency = InAppMessageFrequency.ONLY_ONCE.value)
        )
        every { bitmapCache.has(any()) } returns true
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1000), null)
        assertNull(manager.getRandom("session_start", hashMapOf(), null))
    }

    @Test
    fun `should apply 'until_visitor_interacts' frequency filter`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(frequency = InAppMessageFrequency.UNTIL_VISITOR_INTERACTS.value)
        )
        every { bitmapCache.has(any()) } returns true
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1000), null)
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1000), Date(1000))
        assertNull(manager.getRandom("session_start", hashMapOf(), null))
    }

    @Test
    fun `should apply 'once_per_visit' frequency filter`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(frequency = InAppMessageFrequency.ONCE_PER_VISIT.value)
        )
        every { bitmapCache.has(any()) } returns true
        manager.sessionStarted(Date(1000))
        every { inAppMessageDisplayStateRepository.get(any()) } returns InAppMessageDisplayState(Date(1500), null)
        assertNull(manager.getRandom("session_start", hashMapOf(), null))
        manager.sessionStarted(Date(2000))
        assertNotNull(manager.getRandom("session_start", hashMapOf(), null))
    }

    @Test
    fun `should set message displayed and interacted`() {
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage())
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val delegate = spyk<InAppMessageTrackingDelegate>()
        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<() -> Unit>()
        every {
            presenter.show(any(), any(), any(), any(), capture(actionCallbackSlot), capture(dismissedCallbackSlot))
        } returns mockk()

        waitForIt { manager.preload { it() } }
        runBlocking { manager.showRandom("session_start", hashMapOf(), null, delegate)?.join() }
        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) { inAppMessageDisplayStateRepository.setDisplayed(any(), any()) }
        actionCallbackSlot.captured.invoke(mockActivity, InAppMessageTest.getInAppMessage().payload!!.buttons!![0])
        verify(exactly = 1) { inAppMessageDisplayStateRepository.setInteracted(any(), any()) }
    }

    @Test
    fun `should track dialog lifecycle`() {
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage())
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val delegate = spyk<InAppMessageTrackingDelegate>()
        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<() -> Unit>()
        every {
            presenter.show(any(), any(), any(), any(), capture(actionCallbackSlot), capture(dismissedCallbackSlot))
        } returns mockk()

        waitForIt { manager.preload { it() } }

        runBlocking { manager.showRandom("session_start", hashMapOf(), null, delegate)?.join() }

        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "show", false) }
        actionCallbackSlot.captured.invoke(mockActivity, InAppMessageTest.getInAppMessage().payload!!.buttons!![0])
        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "click", true, "Action") }
        dismissedCallbackSlot.captured.invoke()
        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "close", false) }
    }

    @Test
    fun `should open deeplink once button is clicked`() {
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage())
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        val actionCallbackSlot = slot<(Activity, InAppMessagePayloadButton) -> Unit>()
        every { presenter.show(any(), any(), any(), any(), capture(actionCallbackSlot), any()) } returns mockk()
        waitForIt { manager.preload { it() } }
        runBlocking { manager.showRandom("session_start", hashMapOf(), null, spyk())?.join() }
        Robolectric.flushForegroundThreadScheduler()

        val button = InAppMessageTest.getInAppMessage().payload!!.buttons!![0]
        assertNull(shadowOf(mockActivity).nextStartedActivityForResult)
        actionCallbackSlot.captured.invoke(mockActivity, button)
        assertEquals(
            button.buttonLink,
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
            eventManager.track(capture(eventTypeSlot), any(), capture(propertiesSlot), capture(typeSlot))
        } just Runs

        val delegate = EventManagerInAppMessageTrackingDelegate(
            ApplicationProvider.getApplicationContext(),
            eventManager
        )
        delegate.track(InAppMessageTest.getInAppMessage(), "mock-action", false)
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
    fun `should preload pending message image first and show it`() {
        val pendingMessage = InAppMessageTest.getInAppMessage(
            trigger = EventFilter("session_start", arrayListOf()),
            imageUrl = "pending_image_url"
        )
        val otherMessage1 = InAppMessageTest.getInAppMessage(
            trigger = EventFilter("other_event", arrayListOf()),
            imageUrl = "other_image_url_1"
        )
        val otherMessage2 = InAppMessageTest.getInAppMessage(
            trigger = EventFilter("other_event", arrayListOf()),
            imageUrl = "other_image_url_2"
        )
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(pendingMessage, otherMessage1, otherMessage2))
            )
        }
        every { bitmapCache.preload(any(), any()) } answers { secondArg<((Boolean) -> Unit)?>()?.invoke(true) }
        every { messagesCache.get() } returns arrayListOf(pendingMessage, otherMessage1, otherMessage2)
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        every { bitmapCache.has(any()) } answers { firstArg<String>() == "pending_image_url" }

        manager.showRandom("session_start", hashMapOf(), null, spyk())

        waitForIt { manager.preload { _ -> it() } }

        verifySequence {
            messagesCache.set(arrayListOf(pendingMessage, otherMessage1, otherMessage2))
            bitmapCache.clearExcept(arrayListOf("pending_image_url", "other_image_url_1", "other_image_url_2"))
            messagesCache.get()
            bitmapCache.preload("pending_image_url", any())
            bitmapCache.get("pending_image_url")
            presenter.show(InAppMessageType.MODAL, pendingMessage.payload!!, any(), any(), any(), any())
            bitmapCache.preload("other_image_url_1", any())
            bitmapCache.preload("other_image_url_2", any())
        }
        confirmVerified(messagesCache, bitmapCache, presenter)
    }

    @Test
    fun `should delay in-app message presenting`() {
        val message = InAppMessageTest.getInAppMessage(delay = 1234)
        every { messagesCache.get() } returns arrayListOf(message)
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { presenter.show(any(), any(), any(), any(), any(), any()) } returns mockk()

        waitForIt { manager.preload { it() } }
        runBlocking { manager.showRandom("session_start", hashMapOf(), null, spyk())?.join() }
        Robolectric.getForegroundThreadScheduler().advanceBy(1233, TimeUnit.MILLISECONDS)
        verify(exactly = 0) {
            presenter.show(InAppMessageType.MODAL, message.payload!!, any(), any(), any(), any())
        }
        Robolectric.getForegroundThreadScheduler().advanceBy(1, TimeUnit.MILLISECONDS)
        verify(exactly = 1) {
            presenter.show(InAppMessageType.MODAL, message.payload!!, any(), any(), any(), any())
        }
    }

    @Test
    fun `should pass timeout to in-app message presenter`() {
        val message = InAppMessageTest.getInAppMessage(timeout = 1234)
        every { messagesCache.get() } returns arrayListOf(message)
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { presenter.show(any(), any(), any(), any(), any(), any()) } returns mockk()

        waitForIt { manager.preload { it() } }
        runBlocking { manager.showRandom("session_start", hashMapOf(), null, spyk())?.join() }
        Robolectric.flushForegroundThreadScheduler()
        verify {
            presenter.show(InAppMessageType.MODAL, message.payload!!, any(), 1234, any(), any())
        }
    }

    @Test
    fun `should track control group message without showing it`() {
        val message = InAppMessageTest.getInAppMessage(
            payload = null,
            variantId = -1,
            variantName = "Control group",
            timeout = 1234
        )
        val delegate = spyk<InAppMessageTrackingDelegate>()
        every { messagesCache.get() } returns arrayListOf(message)
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        every { presenter.show(any(), any(), any(), any(), any(), any()) } returns mockk()
        waitForIt { manager.preload { it() } }
        runBlocking { manager.showRandom("session_start", hashMapOf(), null, delegate) }
        Robolectric.flushForegroundThreadScheduler()
        verify(exactly = 1) { delegate.track(message, "show", false) }
        verify(exactly = 0) { presenter.show(any(), any(), any(), any(), any(), any()) }
    }
}
