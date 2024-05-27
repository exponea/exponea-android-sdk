package com.exponea.sdk.manager

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.models.eventfilter.EventFilter
import com.exponea.sdk.network.ExponeaServiceImpl
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.repository.InAppMessagesCacheImpl
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import com.exponea.sdk.util.runOnBackgroundThread
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mock.HttpCode
import okhttp3.mock.MockInterceptor
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageManagerFlowTest : ExponeaSDKTest() {

    @Before
    fun disableFetchData() {
        every { anyConstructed<FetchManagerImpl>().fetchConsents(any(), any(), any()) } answers {
            secondArg<(Result<ArrayList<Consent>>) -> Unit>().invoke(
                Result(true, arrayListOf())
            )
        }
        every { anyConstructed<FetchManagerImpl>().fetchRecommendation(any(), any(), any(), any()) } answers {
            thirdArg<(Result<ArrayList<CustomerRecommendation>>) -> Unit>().invoke(
                Result(true, arrayListOf())
            )
        }
        every { anyConstructed<FetchManagerImpl>().fetchAppInbox(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(3).invoke(
                Result(true, arrayListOf())
            )
        }
        every {
            anyConstructed<FetchManagerImpl>().fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(
                Result(true, arrayListOf())
            )
        }
        every { anyConstructed<FetchManagerImpl>().fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(true, arrayListOf())
            )
        }
        every {
            anyConstructed<FetchManagerImpl>().markAppInboxAsRead(any(), any(), any(), any(), any(), any())
        } answers {
            arg<(Result<Any?>) -> Unit>(4).invoke(
                Result(true, null)
            )
        }
        every { anyConstructed<FetchManagerImpl>().fetchInAppMessages(any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppMessage>>) -> Unit>(2).invoke(
                Result(true, arrayListOf())
            )
        }
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(
                Result(true, SegmentationCategories())
            )
        }
        mockkConstructorFix(FcmManagerImpl::class) {
            every { anyConstructed<FcmManagerImpl>().trackToken(any(), any(), any()) }
        }
        mockkConstructorFix(TimeLimitedFcmManagerImpl::class) {
            every { anyConstructed<TimeLimitedFcmManagerImpl>().trackToken(any(), any(), any()) }
        }
        every { anyConstructed<FcmManagerImpl>().trackToken(any(), any(), any()) } just Runs
        every { anyConstructed<TimeLimitedFcmManagerImpl>().trackToken(any(), any(), any()) } just Runs
    }

    @Before
    fun disableRealUpload() {
        val mockServer = ExponeaMockServer.createServer()
        val mockUrl = mockServer.url("/").toString()
        val mockInterceptor = MockInterceptor().apply {
            addRule()
                .get().or().post().or().put()
                .url(mockUrl)
                .anyTimes()
                .respond(HttpCode.HTTP_200_OK, null)
        }
        val okHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(mockInterceptor)
            .build()
        every { anyConstructed<ExponeaServiceImpl>().doPost(any(), any<String>(), any()) } answers {
            okHttpClient.newCall(Request.Builder().url(mockUrl).get().build())
        }
    }

    @Before
    fun prepareInAppMocks() {
        mockkConstructorFix(InAppMessageBitmapCacheImpl::class) {
            every { anyConstructed<InAppMessageBitmapCacheImpl>().preload(any(), any()) }
        }
        mockkConstructorFix(InAppMessagesCacheImpl::class)
    }

    @Before
    fun disableSegmentsManager() {
        mockkConstructorFix(SegmentsManagerImpl::class)
        every { anyConstructed<SegmentsManagerImpl>().onEventUploaded(any()) } just Runs
        every { anyConstructed<SegmentsManagerImpl>().onSdkInit() } just Runs
        every { anyConstructed<SegmentsManagerImpl>().onCallbackAdded(any()) } just Runs
    }

    @Before
    fun disableInAppContentBlockManager() {
        mockkConstructorFix(InAppContentBlocksManagerImpl::class)
        every { anyConstructed<InAppContentBlocksManagerImpl>().onEventCreated(any(), any()) } just Runs
    }

    @Test
    fun `should preload and show for session_start for IMMEDIATE flush with delay`() {
        val threadAwaitSeconds = 10L
        Exponea.flushMode = FlushMode.IMMEDIATE
        Exponea.loggerLevel = Logger.Level.VERBOSE
        // allow process
        every { anyConstructed<InAppMessageManagerImpl>().detectReloadMode(any(), any()) } answers { callOriginal() }
        every { anyConstructed<InAppMessageManagerImpl>().pickAndShowMessage() } answers { callOriginal() }
        // disabled real In-app fetch
        val pendingMessage = InAppMessageTest.getInAppMessage(
            trigger = EventFilter(Constants.EventTypes.sessionStart, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "12345"
        )
        prepareMessagesMocks(arrayListOf(pendingMessage))
        initSdk()
        // ensure that identifyCustomer upload is done after session_start process
        val sessionStartProcessed = CountDownLatch(1)
        val identifyCustomerProcessed = CountDownLatch(1)
        every {
            // only identifyCustomer could invoke this
            anyConstructed<InAppMessageManagerImpl>().onEventUploaded(any())
        } answers {
            val isCustomerUploaded = firstArg<ExportedEvent>().sdkEventType == EventType.TRACK_CUSTOMER.name
            if (isCustomerUploaded) {
                assertTrue(sessionStartProcessed.await(threadAwaitSeconds, TimeUnit.SECONDS))
            }
            callOriginal()
            if (isCustomerUploaded) {
                identifyCustomerProcessed.countDown()
            }
        }
        every {
            anyConstructed<InAppMessageManagerImpl>().onEventCreated(any(), any())
        } answers {
            callOriginal()
            if (arg<EventType>(1) == EventType.SESSION_START) {
                sessionStartProcessed.countDown()
            }
        }
        // invoke test scenario
        val customerIdsMap: HashMap<String, String?> = hashMapOf("registered" to "test001")
        identifyCustomerForTest(customerIdsMap)
        Exponea.trackSessionStart()
        assertTrue(sessionStartProcessed.await(threadAwaitSeconds, TimeUnit.SECONDS))
        assertTrue(identifyCustomerProcessed.await(threadAwaitSeconds, TimeUnit.SECONDS))
        verify(exactly = 1) {
            anyConstructed<InAppMessageManagerImpl>().show(pendingMessage)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should preload and show for each session_start`() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Main)
        Exponea.flushMode = FlushMode.MANUAL
        // allow process
        every { anyConstructed<InAppMessageManagerImpl>().detectReloadMode(any(), any()) } answers { callOriginal() }
        every { anyConstructed<InAppMessageManagerImpl>().pickAndShowMessage() } answers { callOriginal() }
        // disabled real In-app fetch
        val pendingMessage = InAppMessageTest.getInAppMessage(
            trigger = EventFilter(Constants.EventTypes.sessionStart, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "12345"
        )
        prepareMessagesMocks(arrayListOf(pendingMessage))
        initSdk()
        simulateCustomerUsage(hashMapOf("registered" to "test001"))
        simulateCustomerUsage(hashMapOf("registered" to "test002"))
        simulateCustomerUsage(hashMapOf("registered" to "test003"))
        verify(exactly = 3) {
            anyConstructed<InAppMessageManagerImpl>().show(any())
        }
    }

    @After
    fun resetThreadBehaviour() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Default)
    }

    @Test
    fun `should show message only for last identifyCustomer for MANUAL flush`() {
        Exponea.flushMode = FlushMode.MANUAL
        // allow process
        every { anyConstructed<InAppMessageManagerImpl>().detectReloadMode(any(), any()) } answers { callOriginal() }
        every { anyConstructed<InAppMessageManagerImpl>().pickAndShowMessage() } answers { callOriginal() }
        prepareMessagesMocks(arrayListOf())
        initSdk()

        // login customerA, message pendingMessageA has to be loaded
        val pendingMessageA = InAppMessageTest.getInAppMessage(
            trigger = EventFilter(Constants.EventTypes.sessionStart, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "12345A"
        )
        Exponea.anonymize()
        prepareMessagesMocks(arrayListOf(pendingMessageA))
        identifyCustomerForTest(hashMapOf("registered" to "customerA"))
        // login customerB, message pendingMessageB has to be loaded
        val pendingMessageB = InAppMessageTest.getInAppMessage(
            trigger = EventFilter(Constants.EventTypes.sessionStart, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "12345B"
        )
        Exponea.anonymize()
        prepareMessagesMocks(arrayListOf(pendingMessageB))
        identifyCustomerForTest(hashMapOf("registered" to "customerB"))
        // check that pendingMessageB is going to show for customerB
        val messageSlot = slot<InAppMessage>()
        every { anyConstructed<InAppMessageManagerImpl>().show(capture(messageSlot)) } just Runs
        Exponea.trackSessionStart()
        Thread.sleep(2000)
        assert(messageSlot.isCaptured)
        assertNotNull(messageSlot.captured)
        assertEquals(pendingMessageB.id, messageSlot.captured.id)
    }

    @Test
    fun `should try to show message for unloaded image`() {
        Exponea.flushMode = FlushMode.MANUAL
        // allow process
        every { anyConstructed<InAppMessageManagerImpl>().detectReloadMode(any(), any()) } answers { callOriginal() }
        every { anyConstructed<InAppMessageManagerImpl>().pickAndShowMessage() } answers { callOriginal() }
        // disabled real In-app fetch
        val pendingMessage = InAppMessageTest.getInAppMessage(
            trigger = EventFilter(Constants.EventTypes.sessionStart, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "12345"
        )
        prepareMessagesMocks(arrayListOf(pendingMessage))
        disableBitmapCache()
        initSdk()
        simulateCustomerUsage(hashMapOf("registered" to "test001"))
        verify(exactly = 1) {
            anyConstructed<InAppMessageManagerImpl>().trackError(pendingMessage, "Images has not been preloaded")
        }
        verify(exactly = 0) {
            anyConstructed<InAppMessageManagerImpl>().show(any())
        }
    }

    @Test
    fun `should not show message for session_start if another customer identifies`() {
        val threadAwaitSeconds = 5L
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.loggerLevel = Logger.Level.VERBOSE
        // allow process
        every { anyConstructed<InAppMessageManagerImpl>().detectReloadMode(any(), any()) } answers { callOriginal() }
        every { anyConstructed<InAppMessageManagerImpl>().pickAndShowMessage() } answers { callOriginal() }
        // disabled real In-app fetch
        val pendingMessage = InAppMessageTest.getInAppMessage(
            trigger = EventFilter(Constants.EventTypes.sessionStart, arrayListOf()),
            imageUrl = "pending_image_url",
            priority = null,
            id = "12345"
        )
        prepareMessagesMocks(arrayListOf(pendingMessage))
        initSdk()
        identifyCustomerForTest(hashMapOf("registered" to "test001"))
        // ensure that session_start picks message but image load finishes after another customer login
        val sessionStartProcessed = CountDownLatch(1)
        val identifyCustomerProcessed = CountDownLatch(1)
        every {
            anyConstructed<InAppMessageManagerImpl>().preloadAndShow(any(), any())
        } answers {
            Logger.e(this, "[InApp] SessionStart preloadAndShow occurs")
            assertTrue(identifyCustomerProcessed.await(threadAwaitSeconds, TimeUnit.SECONDS))
            Logger.e(this, "[InApp] SessionStart waiting for identifyCustomerProcessed ends")
            sessionStartProcessed.countDown()
            callOriginal()
        }
        every {
            anyConstructed<InAppMessageManagerImpl>().onEventCreated(any(), any())
        } answers {
            if (arg<EventType>(1) == EventType.TRACK_CUSTOMER) {
                Logger.e(this, "[InApp] IdentifyCustomer onEventCreated occurs")
                identifyCustomerProcessed.countDown()
            }
            callOriginal()
        }
        // invoke test scenario
        Exponea.trackSessionStart()
        runOnBackgroundThread {
            Exponea.anonymize()
            identifyCustomerForTest(hashMapOf("registered" to "test002"))
        }
        assertTrue(sessionStartProcessed.await(threadAwaitSeconds, TimeUnit.SECONDS))
        assertTrue(identifyCustomerProcessed.await(threadAwaitSeconds, TimeUnit.SECONDS))
        verify(exactly = 0) {
            anyConstructed<InAppMessageManagerImpl>().show(pendingMessage)
        }
    }

    private fun disableBitmapCache() {
        every {
            anyConstructed<InAppMessageBitmapCacheImpl>().preload(any(), any())
        } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(false)
        }
        every { anyConstructed<InAppMessageBitmapCacheImpl>().get(any()) } returns null
        every { anyConstructed<InAppMessageBitmapCacheImpl>().has(any()) } returns false
    }

    private fun prepareMessagesMocks(pendingMessages: ArrayList<InAppMessage>) {
        every { anyConstructed<FetchManagerImpl>().fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, pendingMessages)
            )
        }
        every {
            anyConstructed<InAppMessageBitmapCacheImpl>().preload(any(), any())
        } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(true)
        }
        every {
            anyConstructed<InAppMessageBitmapCacheImpl>().get(any())
        } returns BitmapFactory.decodeFile("mock-file")
        every {
            anyConstructed<InAppMessageBitmapCacheImpl>().has(any())
        } answers {
            firstArg<String>() == "pending_image_url"
        }
        every { anyConstructed<InAppMessagesCacheImpl>().get() } returns pendingMessages
        every { anyConstructed<InAppMessagesCacheImpl>().set(any()) } just Runs
        every { anyConstructed<InAppMessagesCacheImpl>().clear() } returns true
        every { anyConstructed<InAppMessagesCacheImpl>().getTimestamp() } returns System.currentTimeMillis()
    }

    private fun simulateCustomerUsage(customerIdsMap: java.util.HashMap<String, String?>) {
        Exponea.anonymize()
        identifyCustomerForTest(customerIdsMap)
        Exponea.trackSessionStart()
        Exponea.trackSessionEnd()
    }

    private fun identifyCustomerForTest(
        customerIdsMap: HashMap<String, String?>,
        properties: HashMap<String, Any> = hashMapOf()
    ) {
        val customerIds = CustomerIds(customerIdsMap).apply {
            cookie = customerIdsMap[CustomerIds.COOKIE]
        }
        Exponea.identifyCustomer(
            customerIds = customerIds,
            properties = PropertiesList(properties)
        )
    }

    private fun initSdk() {
        skipInstallEvent()
        val initialProject = ExponeaProject(
            "https://base-url.com",
            "project-token",
            "Token auth"
        )
        Exponea.init(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                baseURL = initialProject.baseUrl,
                projectToken = initialProject.projectToken,
                authorization = initialProject.authorization,
                automaticSessionTracking = false
            )
        )
    }
}
