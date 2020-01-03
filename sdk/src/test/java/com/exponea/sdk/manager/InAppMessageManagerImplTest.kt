package com.exponea.sdk.manager

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DateFilter
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.InAppMessageTrigger
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
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

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageManagerImplTest {
    private lateinit var fetchManager: FetchManager
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var messagesCache: InAppMessagesCache
    private lateinit var bitmapCache: InAppMessageBitmapCache
    private lateinit var presenter: InAppMessageDialogPresenter
    private lateinit var manager: InAppMessageManagerImpl

    @Before
    fun before() {
        fetchManager = mockk()
        messagesCache = mockk()
        every { messagesCache.set(any()) } just Runs
        bitmapCache = mockk()
        every { bitmapCache.has(any()) } returns false
        every { bitmapCache.preload(any(), any()) } just Runs
        every { bitmapCache.clearExcept(any()) } just Runs
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds()
        presenter = mockk()
        every { presenter.show(any(), any(), any(), any()) } returns true
        manager = InAppMessageManagerImpl(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(),
            customerIdsRepository,
            messagesCache,
            fetchManager,
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
    fun `should get null if no messages available`() {
        every { messagesCache.get() } returns arrayListOf()
        assertNull(manager.getRandom("testEvent"))
    }

    @Test
    fun `should get message if both message and bitmap available`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(),
            InAppMessageTest.getInAppMessage()
        )
        every { bitmapCache.has(any()) } returns true
        assertEquals(InAppMessageTest.getInAppMessage(), manager.getRandom("session_start"))
    }

    @Test
    fun `should not get message if bitmap is not available`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(),
            InAppMessageTest.getInAppMessage()
        )
        assertEquals(null, manager.getRandom("session_start"))
    }

    @Test
    fun `should apply date filter`() {
        every { bitmapCache.has(any()) } returns true
        val setupStoredEvent = { dateFilter: DateFilter ->
            every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage(dateFilter = dateFilter))
        }

        val currentTime = (System.currentTimeMillis() / 1000).toInt()

        setupStoredEvent(DateFilter(true, null, null))
        assertNotNull(manager.getRandom("session_start"))

        setupStoredEvent(DateFilter(true, null, currentTime - 100))
        assertNull(manager.getRandom("session_start"))

        setupStoredEvent(DateFilter(true, null, currentTime + 100))
        assertNotNull(manager.getRandom("session_start"))

        setupStoredEvent(DateFilter(true, currentTime + 100, null))
        assertNull(manager.getRandom("session_start"))

        setupStoredEvent(DateFilter(true, currentTime - 100, null))
        assertNotNull(manager.getRandom("session_start"))

        setupStoredEvent(DateFilter(true, currentTime - 100, currentTime + 100))
        assertNotNull(manager.getRandom("session_start"))

        setupStoredEvent(DateFilter(true, currentTime + 100, currentTime + 100))
        assertNull(manager.getRandom("session_start"))

        setupStoredEvent(DateFilter(false, currentTime + 100, currentTime + 100))
        assertNotNull(manager.getRandom("session_start"))
    }

    @Test
    fun `should apply event filter`() {
        every { bitmapCache.has(any()) } returns true
        val setupStoredEvent = { trigger: InAppMessageTrigger ->
            every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage(trigger = trigger))
        }

        setupStoredEvent(InAppMessageTrigger(type = null, eventType = null))
        assertNull(manager.getRandom("session_start"))

        setupStoredEvent(InAppMessageTrigger(type = "event", eventType = "session_start"))
        assertNotNull(manager.getRandom("session_start"))

        setupStoredEvent(InAppMessageTrigger(type = "event", eventType = "payment"))
        assertNull(manager.getRandom("session_start"))

        setupStoredEvent(InAppMessageTrigger(type = "event", eventType = "payment"))
        assertNotNull(manager.getRandom("payment"))
    }

    @Test
    fun `should track dialog lifecycle`() {
        every { messagesCache.get() } returns arrayListOf(InAppMessageTest.getInAppMessage())
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.get(any()) } returns BitmapFactory.decodeFile("mock-file")
        val delegate = spyk<InAppMessageTrackingDelegate>()
        val actionCallbackSlot = slot<() -> Unit>()
        val dismissedCallbackSlot = slot<() -> Unit>()
        every { presenter.show(any(), any(), capture(actionCallbackSlot), capture(dismissedCallbackSlot)) } returns true

        runBlocking {
            manager.showRandom("session_start", delegate).join()
        }

        Robolectric.flushForegroundThreadScheduler()

        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "show", false) }
        actionCallbackSlot.captured.invoke()
        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "click", true) }
        dismissedCallbackSlot.captured.invoke()
        verify(exactly = 1) { delegate.track(InAppMessageTest.getInAppMessage(), "close", false) }
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
