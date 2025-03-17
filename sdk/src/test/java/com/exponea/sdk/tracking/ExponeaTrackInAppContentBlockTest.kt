package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.InAppContentBlockManagerImpl
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.runInSingleThread
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class ExponeaTrackInAppContentBlockTest : ExponeaSDKTest() {
    @Before
    fun before() {
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        mockkConstructorFix(FetchManagerImpl::class) {
            every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) }
        }
        mockkConstructorFix(CustomerIdsRepositoryImpl::class)
        mockkConstructorFix(DrawableCacheImpl::class) {
            every { anyConstructed<DrawableCacheImpl>().has(any()) }
        }
        every {
            anyConstructed<DrawableCacheImpl>().has(any())
        } returns true
        every {
            anyConstructed<DrawableCacheImpl>().preload(any<List<String>>(), any())
        } answers {
            arg<(Result<Boolean>) -> Unit>(1).invoke(Result(true, true))
        }
        every {
            anyConstructed<DrawableCacheImpl>().getFile(any())
        } returns MockFile()
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

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track action message for original InAppContentBlock`() = runInSingleThread { idleThreads ->
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        val firstCustomerIds = CustomerIds("brownie").withId("registered", "test1")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns firstCustomerIds
        val placeholderId = "ph1"
        val messageId = "id1"
        every {
            anyConstructed<FetchManagerImpl>().fetchStaticInAppContentBlocks(any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                InAppContentBlockManagerImplTest.buildMessage(
                    messageId,
                    placeholders = listOf(placeholderId),
                    dateFilter = null
                )
            )))
        }
        every {
            anyConstructed<FetchManagerImpl>().fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                // htmlContent
                InAppContentBlockManagerImplTest.buildMessageData(
                    messageId,
                    type = "html",
                    hasTrackingConsent = true,
                    data = mapOf(
                        "html" to InAppContentBlockManagerImplTest.buildHtmlMessageContent()
                    )
                )
            )))
        }
        // turn init-load on
        every { anyConstructed<InAppContentBlockManagerImpl>().loadInAppContentBlockPlaceholders() } answers {
            callOriginal()
        }
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        assertTrue(
            (Exponea.componentForTesting.inAppContentBlockManager as InAppContentBlockManagerImpl)
                .contentBlocksData
                .isNotEmpty()
        )
        // get view and simulate action click
        val view = Exponea.getInAppContentBlocksPlaceholder(placeholderId, ApplicationProvider.getApplicationContext())
        assertNotNull(view)
        val controller = view.controller
        assertNotNull(controller)
        // validate show event
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        controller.onUrlClick("https://exponea.com")
        // validate click event and show next
        verify(exactly = 2) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals("banner", eventSlot.captured.type)
        assertEquals(EventType.BANNER, eventTypeSlot.captured)
        assertEquals(firstCustomerIds.toHashMap(), eventSlot.captured.customerIds)
        val secondCustomerIds = CustomerIds("brownie2").withId("registered", "test2")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns secondCustomerIds
        Exponea.identifyCustomer(secondCustomerIds, PropertiesList(hashMapOf()))
        // validate that customerIDs not changed
        controller.onUrlClick("https://exponea.com")
        assertEquals("banner", eventSlot.captured.type)
        assertEquals(EventType.BANNER, eventTypeSlot.captured)
        assertEquals(firstCustomerIds.toHashMap(), eventSlot.captured.customerIds)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should NOT track action message for FALSE tracking consent`() = runInSingleThread { idleThreads ->
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        } just Runs
        val firstCustomerIds = CustomerIds("brownie").withId("registered", "test1")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns firstCustomerIds
        val placeholderId = "ph1"
        val messageId = "id1"
        every {
            anyConstructed<FetchManagerImpl>().fetchStaticInAppContentBlocks(any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                InAppContentBlockManagerImplTest.buildMessage(
                    messageId,
                    placeholders = listOf(placeholderId),
                    dateFilter = null
                )
            )))
        }
        every {
            anyConstructed<FetchManagerImpl>().fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                // htmlContent
                InAppContentBlockManagerImplTest.buildMessageData(
                    messageId,
                    type = "html",
                    hasTrackingConsent = false,
                    data = mapOf(
                        "html" to InAppContentBlockManagerImplTest.buildHtmlMessageContent()
                    )
                )
            )))
        }
        // turn init-load on
        every {
            anyConstructed<InAppContentBlockManagerImpl>().loadInAppContentBlockPlaceholders()
        } answers {
            callOriginal()
        }
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        assertTrue(
            (Exponea.componentForTesting.inAppContentBlockManager as InAppContentBlockManagerImpl)
                .contentBlocksData
                .isNotEmpty()
        )
        // get view and simulate action click
        val view = Exponea.getInAppContentBlocksPlaceholder(placeholderId, ApplicationProvider.getApplicationContext())
        assertNotNull(view)
        val controller = view.controller
        assertNotNull(controller)
        controller.onUrlClick("https://exponea.com")
        // validate
        verify(exactly = 0) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), true)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track action message for TRUE tracking consent`() = runInSingleThread { idleThreads ->
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        } just Runs
        val firstCustomerIds = CustomerIds("brownie").withId("registered", "test1")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns firstCustomerIds
        val placeholderId = "ph1"
        val messageId = "id1"
        every {
            anyConstructed<FetchManagerImpl>().fetchStaticInAppContentBlocks(any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                InAppContentBlockManagerImplTest.buildMessage(
                    messageId,
                    placeholders = listOf(placeholderId),
                    dateFilter = null
                )
            )))
        }
        every {
            anyConstructed<FetchManagerImpl>().fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3)
                .invoke(Result(true, arrayListOf(
                    // htmlContent
                    InAppContentBlockManagerImplTest.buildMessageData(
                        messageId,
                        type = "html",
                        hasTrackingConsent = true,
                        data = mapOf(
                            "html" to InAppContentBlockManagerImplTest.buildHtmlMessageContent()
                        )
                    )
            )))
        }
        // turn init-load on
        every {
            anyConstructed<InAppContentBlockManagerImpl>().loadInAppContentBlockPlaceholders()
        } answers {
            callOriginal()
        }
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        assertTrue(
            (Exponea.componentForTesting.inAppContentBlockManager as InAppContentBlockManagerImpl)
                .contentBlocksData
                .isNotEmpty()
        )
        // get view and simulate action click
        val view = Exponea.getInAppContentBlocksPlaceholder(placeholderId, ApplicationProvider.getApplicationContext())
        assertNotNull(view)
        val controller = view.controller
        assertNotNull(controller)
        // validate show
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), true)
        }
        controller.onUrlClick("https://exponea.com")
        // validate click and show next
        verify(exactly = 2) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), true)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should DO track action message for FALSE consent but forced action`() = runInSingleThread { idleThreads ->
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        } just Runs
        val firstCustomerIds = CustomerIds("brownie").withId("registered", "test1")
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns firstCustomerIds
        val placeholderId = "ph1"
        val messageId = "id1"
        every {
            anyConstructed<FetchManagerImpl>().fetchStaticInAppContentBlocks(any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                InAppContentBlockManagerImplTest.buildMessage(
                    messageId,
                    placeholders = listOf(placeholderId),
                    dateFilter = null
                )
            )))
            idleThreads()
        }
        every {
            anyConstructed<FetchManagerImpl>().fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3)
                .invoke(Result(true, arrayListOf(
                    // htmlContent
                    InAppContentBlockManagerImplTest.buildMessageData(
                        messageId,
                        type = "html",
                        hasTrackingConsent = false,
                        data = mapOf(
                            "html" to InAppContentBlockManagerImplTest.buildHtmlMessageContent()
                        )
                    )
            )))
            idleThreads()
        }
        // turn init-load on
        every {
            anyConstructed<InAppContentBlockManagerImpl>().loadInAppContentBlockPlaceholders()
        } answers {
            idleThreads()
            callOriginal()
            idleThreads()
        }
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        assertTrue(
            (Exponea.componentForTesting.inAppContentBlockManager as InAppContentBlockManagerImpl)
                .contentBlocksData
                .isNotEmpty()
        )
        // get view and simulate action click
        val view = Exponea.getInAppContentBlocksPlaceholder(placeholderId, ApplicationProvider.getApplicationContext())
        idleThreads()
        assertNotNull(view)
        val controller = view.controller
        assertNotNull(controller)
        idleThreads()
        // validate show without tracking
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), false)
        }
        controller.onUrlClick("https://exponea.com?xnpe_force_track=true")
        idleThreads()
        // validate click with forced tracking
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), true)
        }
    }
}
