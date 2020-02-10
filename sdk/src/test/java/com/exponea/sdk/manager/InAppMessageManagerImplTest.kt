package com.exponea.sdk.manager

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DateFilter
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageDisplayState
import com.exponea.sdk.models.InAppMessageFrequency
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.eventfilter.EventFilter
import com.exponea.sdk.models.eventfilter.EventPropertyFilter
import com.exponea.sdk.models.eventfilter.StringConstraint
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.repository.InAppMessageDisplayStateRepository
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.view.InAppMessageDialogPresenter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    private lateinit var presenter: InAppMessageDialogPresenter
    private lateinit var manager: InAppMessageManagerImpl

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
        every { presenter.show(any(), any(), any(), any(), any()) } returns mockk()
        manager = InAppMessageManagerImpl(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(),
            customerIdsRepository,
            messagesCache,
            fetchManager,
            inAppMessageDisplayStateRepository,
            bitmapCache,
            presenter
        )
    }

    @After
    fun after() {
        unmockkAll()
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
        waitForIt {
            manager.preload { result ->
                it.assertTrue(result.isSuccess)
                verify(exactly = 1) { messagesCache.set(arrayListOf(InAppMessageTest.getInAppMessage())) }
                it()
            }
        }
    }

    @Test
    fun `should reload messages on session start`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(Result(true, arrayListOf()))
        }
        waitForIt { manager.preload { _ -> it() } }
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        manager.sessionStarted(Date())
        verify(exactly = 1) { fetchManager.fetchInAppMessages(any(), any(), any(), any()) }
        manager.sessionStarted(Date(Date().time + InAppMessageManagerImpl.REFRESH_CACHE_AFTER * 2))
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
        val delegate = spyk<InAppMessageTrackingDelegate>()
        val actionCallbackSlot = slot<(InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<() -> Unit>()
        every {
            presenter.show(any(), any(), any(), capture(actionCallbackSlot), capture(dismissedCallbackSlot))
        } returns mockk()

        runBlocking { manager.showRandom("session_start", hashMapOf(), null, delegate).join() }
        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) { inAppMessageDisplayStateRepository.setDisplayed(any(), any()) }
        actionCallbackSlot.captured.invoke(InAppMessageTest.getInAppMessage().payload.buttons!![0])
        verify(exactly = 1) { inAppMessageDisplayStateRepository.setInteracted(any(), any()) }
    }

    @Test
    fun `should track dialog lifecycle`() {
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage())
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        val delegate = spyk<InAppMessageTrackingDelegate>()
        val actionCallbackSlot = slot<(InAppMessagePayloadButton) -> Unit>()
        val dismissedCallbackSlot = slot<() -> Unit>()
        every {
            presenter.show(any(), any(), any(), capture(actionCallbackSlot), capture(dismissedCallbackSlot))
        } returns mockk()

        runBlocking {
            manager.showRandom("session_start", hashMapOf(), null, delegate).join()
        }

        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "show", false) }
        actionCallbackSlot.captured.invoke(InAppMessageTest.getInAppMessage().payload.buttons!![0])
        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "click", true) }
        dismissedCallbackSlot.captured.invoke()
        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "close", false) }
    }

    @Test
    fun `should open deeplink once button is clicked`() {
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage())
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        val actionCallbackSlot = slot<(InAppMessagePayloadButton) -> Unit>()
        every { presenter.show(any(), any(), any(), capture(actionCallbackSlot), any()) } returns mockk()

        runBlocking { manager.showRandom("session_start", hashMapOf(), null, spyk()).join() }
        Robolectric.flushForegroundThreadScheduler()

        val activity = Activity()
        val button = InAppMessageTest.getInAppMessage().payload.buttons!![0]
        assertNull(shadowOf(activity).nextStartedActivityForResult)
        actionCallbackSlot.captured.invoke(button)
        assertEquals(
            button.buttonLink,
            shadowOf(activity).nextStartedActivityForResult.intent.data.toString()
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
}
