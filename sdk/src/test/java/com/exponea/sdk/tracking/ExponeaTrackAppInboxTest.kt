package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.AppInboxManagerImplTest
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.MessageItemAction
import com.exponea.sdk.models.MessageItemAction.Type.BROWSER
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.AppInboxCacheImplTest
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class ExponeaTrackAppInboxTest : ExponeaSDKTest() {
    @Before
    fun before() {
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        mockkConstructorFix(FetchManagerImpl::class) {
            every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) }
        }
        mockkConstructorFix(CustomerIdsRepositoryImpl::class)
        skipInstallEvent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            authorization = "Token mock-auth"
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
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
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should enhance fetched AppInbox messages`() {
        val receivedSyncToken = "sync_123"
        val currentCustomerIds = CustomerIds().withId("registered", "test")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns currentCustomerIds
        every {
            anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            AppInboxManagerImplTest.buildMessage("id1", type = "push"),
                            AppInboxManagerImplTest.buildMessage("id2", type = "html")
                        ),
                        receivedSyncToken
                    )
                )
        }
        var fetchedMessages: List<MessageItem> = listOf()
        Exponea.fetchAppInbox { data ->
            fetchedMessages = data ?: listOf()
        }
        assertEquals(2, fetchedMessages.size)
        fetchedMessages.forEach { msg ->
            assertEquals(receivedSyncToken, msg.syncToken)
            assertEquals(currentCustomerIds.toHashMap(), msg.customerIds)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track opened message for original AppInbox`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        val firstSyncToken = "sync_123"
        val firstCustomerIds = CustomerIds().withId("registered", "test1")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns firstCustomerIds
        every {
            anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            AppInboxManagerImplTest.buildMessage("id1", type = "push"),
                            AppInboxManagerImplTest.buildMessage("id2", type = "html")
                        ),
                        firstSyncToken
                    )
                )
        }
        var fetchedMessages: List<MessageItem> = listOf()
        Exponea.fetchAppInbox { data ->
            fetchedMessages = data ?: listOf()
        }
        assertEquals(2, fetchedMessages.size)
        fetchedMessages.forEach { msg ->
            assertEquals(firstSyncToken, msg.syncToken)
            assertEquals(firstCustomerIds.toHashMap(), msg.customerIds)
        }
        val firstMessage = fetchedMessages[0]
        // scenario: Details screen is opened for 'firstMessage' but identifyCustomer(another) has been called
        // so new AppInbox has been loaded, but detail screen is kept
        // we need to track for original customerIds
        val secondSyncToken = "sync_1234"
        val secondCustomerIds = CustomerIds().withId("registered", "test2")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns secondCustomerIds
        every {
            anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            AppInboxManagerImplTest.buildMessage("id1", type = "push"),
                            AppInboxManagerImplTest.buildMessage("id2", type = "html")
                        ),
                        secondSyncToken
                    )
                )
        }
        fetchedMessages = listOf()
        Exponea.fetchAppInbox { data ->
            fetchedMessages = data ?: listOf()
        }
        assertEquals(2, fetchedMessages.size)
        fetchedMessages.forEach { msg ->
            assertEquals(secondSyncToken, msg.syncToken)
            assertEquals(secondCustomerIds.toHashMap(), msg.customerIds)
        }
        // AppInbox and Customer changed but track has been invoked from detail screen (for old msg)
        Exponea.trackAppInboxOpened(firstMessage)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals("campaign", eventSlot.captured.type)
        assertEquals(EventType.APP_INBOX_OPENED, eventTypeSlot.captured)
        assertEquals(firstCustomerIds.toHashMap(), eventSlot.captured.customerIds)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track clicked message for original AppInbox`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        val firstSyncToken = "sync_123"
        val firstCustomerIds = CustomerIds().withId("registered", "test1")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns firstCustomerIds
        every {
            anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            AppInboxManagerImplTest.buildMessage("id1", type = "push"),
                            AppInboxManagerImplTest.buildMessage("id2", type = "html")
                        ),
                        firstSyncToken
                    )
                )
        }
        var fetchedMessages: List<MessageItem> = listOf()
        Exponea.fetchAppInbox { data ->
            fetchedMessages = data ?: listOf()
        }
        assertEquals(2, fetchedMessages.size)
        fetchedMessages.forEach { msg ->
            assertEquals(firstSyncToken, msg.syncToken)
            assertEquals(firstCustomerIds.toHashMap(), msg.customerIds)
        }
        val firstMessage = fetchedMessages[0]
        // scenario: Details screen is opened for 'firstMessage' but identifyCustomer(another) has been called
        // so new AppInbox has been loaded, but detail screen is kept
        // we need to track for original customerIds
        val secondSyncToken = "sync_1234"
        val secondCustomerIds = CustomerIds().withId("registered", "test2")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns secondCustomerIds
        every {
            anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            AppInboxManagerImplTest.buildMessage("id1", type = "push"),
                            AppInboxManagerImplTest.buildMessage("id2", type = "html")
                        ),
                        secondSyncToken
                    )
                )
        }
        fetchedMessages = listOf()
        Exponea.fetchAppInbox { data ->
            fetchedMessages = data ?: listOf()
        }
        assertEquals(2, fetchedMessages.size)
        fetchedMessages.forEach { msg ->
            assertEquals(secondSyncToken, msg.syncToken)
            assertEquals(secondCustomerIds.toHashMap(), msg.customerIds)
        }
        // AppInbox and Customer changed but track has been invoked from detail screen (for old msg)
        val action = MessageItemAction().apply {
            type = BROWSER
            title = "Doesnt matter now"
            url = "https://example.com"
        }
        Exponea.trackAppInboxClick(action, firstMessage)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals("campaign", eventSlot.captured.type)
        assertEquals(EventType.APP_INBOX_CLICKED, eventTypeSlot.captured)
        assertEquals(firstCustomerIds.toHashMap(), eventSlot.captured.customerIds)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track clicked message for same fetched AppInbox`() {
        val receivedSyncToken = "sync_123"
        val currentCustomerIds = CustomerIds().withId("registered", "test")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns currentCustomerIds
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        every {
            anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            AppInboxManagerImplTest.buildMessage("id1", type = "push"),
                            AppInboxManagerImplTest.buildMessage("id2", type = "html")
                        ),
                        receivedSyncToken
                    )
                )
        }
        var fetchedMessages: List<MessageItem> = listOf()
        Exponea.fetchAppInbox { data ->
            fetchedMessages = data ?: listOf()
        }
        assertEquals(2, fetchedMessages.size)
        val trackedMessage = fetchedMessages[0]
        val action = MessageItemAction().apply {
            type = BROWSER
            title = "Doesnt matter now"
            url = "https://example.com"
        }
        Exponea.trackAppInboxClick(action, trackedMessage)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals("campaign", eventSlot.captured.type)
        assertEquals(EventType.APP_INBOX_CLICKED, eventTypeSlot.captured)
        assertEquals(currentCustomerIds.toHashMap(), eventSlot.captured.customerIds)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track opened message for same fetched AppInbox`() {
        val receivedSyncToken = "sync_123"
        val currentCustomerIds = CustomerIds().withId("registered", "test")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns currentCustomerIds
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        every {
            anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            AppInboxManagerImplTest.buildMessage("id1", type = "push"),
                            AppInboxManagerImplTest.buildMessage("id2", type = "html")
                        ),
                        receivedSyncToken
                    )
                )
        }
        var fetchedMessages: List<MessageItem> = listOf()
        Exponea.fetchAppInbox { data ->
            fetchedMessages = data ?: listOf()
        }
        assertEquals(2, fetchedMessages.size)
        val trackedMessage = fetchedMessages[0]
        Exponea.trackAppInboxOpened(trackedMessage)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals("campaign", eventSlot.captured.type)
        assertEquals(EventType.APP_INBOX_OPENED, eventTypeSlot.captured)
        assertEquals(currentCustomerIds.toHashMap(), eventSlot.captured.customerIds)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should NOT track opened message for empty customer IDs`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        Exponea.trackAppInboxOpened(AppInboxCacheImplTest.buildMessage("id3"))
        verify(exactly = 0) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should NOT track clicked message for empty customer IDs`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        val action = MessageItemAction().apply {
            type = BROWSER
            title = "Doesnt matter now"
            url = "https://example.com"
        }
        Exponea.trackAppInboxClick(action, AppInboxCacheImplTest.buildMessage("id3"))
        verify(exactly = 0) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
    }
}
