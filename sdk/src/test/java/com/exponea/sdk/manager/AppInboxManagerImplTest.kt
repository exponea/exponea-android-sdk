package com.exponea.sdk.manager

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaNotificationActionType
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.MessageItemAction
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.AppInboxCache
import com.exponea.sdk.repository.AppInboxCacheImpl
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.testutil.waitForIt
import com.google.gson.Gson
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.slot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class AppInboxManagerImplTest : ExponeaSDKTest() {

    companion object {
        public fun buildMessage(
            id: String,
            type: String = "push",
            read: Boolean = true,
            received: Double = System.currentTimeMillis().toDouble(),
            data: Map<String, Any?> = mapOf()
        ): MessageItem {
            return MessageItem(
                id = id,
                rawType = type,
                read = read,
                rawContent =
                    data + mapOf(
                    "attributes" to mapOf(
                        "sent_timestamp" to received
                    )
                )
            )
        }
        /**
         * Copied directly from APP editor, contains 2 actions (deeplink + browser), 1 image, title and message
         */
        public fun buildHtmlMessageContent(): String {
            return """
            <style>
                .in-app-message-wrapper {
                    display: flex;
                    width: 100%;
                    height: 100%;
                    font-family: sans-serif;
                }

                .in-app-message {
                    display: block;
                    position: relative;
                    user-select: none;
                    max-height: 600px;
                    margin: auto 22px;
                    border: 0;
                    border-radius: 8px;
                    box-shadow: 0px 4px 8px rgba(102, 103, 128, 0.25);
                    overflow-y: auto;
                    width: 100%;
                }

                .in-app-message .image {
                    max-height: 160px;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    pointer-events: none;
                }

                .in-app-message .image>img {
                    width: 100%;
                    height: auto;
                }

                .in-app-message .close-icon {
                    display: inline-block;
                    position: absolute;
                    width: 16px;
                    height: 16px;
                    top: 10px;
                    right: 10px;
                    background-color: rgba(250, 250, 250, 0.6);
                    border-radius: 50%;
                    cursor: pointer;
                }

                .in-app-message .close-icon::before,
                .in-app-message .close-icon::after {
                    content: "";
                    height: 11px;
                    width: 2px;
                    position: absolute;
                    top: 2px;
                    left: 7px;
                }

                .in-app-message .close-icon::before {
                    transform: rotate(45deg);
                }

                .in-app-message .close-icon::after {
                    transform: rotate(-45deg);
                }

                .in-app-message .content {
                    display: flex;
                    font-size: 16px;
                    flex-direction: column;
                    align-items: center;
                    padding: 20px 13px;
                }

                .in-app-message .content .title {
                    box-sizing: border-box;
                    font-weight: bold;
                    text-align: center;
                    transition: font-size 300ms ease-in-out;
                }

                .in-app-message .content .body {
                    box-sizing: border-box;
                    text-align: center;
                    word-break: break-word;
                    transition: font-size 300ms ease-in-out;
                    margin-top: 8px;
                }

                .in-app-message .content .buttons {
                    display: flex;
                    width: 100%;
                    justify-content: center;
                    margin-top: 15px;
                }

                .in-app-message .content .buttons .button {
                    max-width: 100%;
                    min-width: 110px;
                    font-size: 14px;
                    text-align: center;
                    border-radius: 4px;
                    padding: 8px;
                    cursor: pointer;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    transition: color, background-color 250ms ease-in-out;
                }

                .in-app-message .content .buttons .button:only-child {
                    margin: 0 auto;
                }

                .in-app-message.modal-in-app-message>.content>.buttons>.button:nth-child(2) {
                    margin-left: 8px;
                }
                
                .css-image {
                    background-image: url('https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg')
                }

            </style>


            <div class="in-app-message-wrapper">
                <div class="in-app-message modal-in-app-message " style="background-color: #ffffff">

                    <div class="image">
                        <img src="https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg" />
                    </div>


                    <style>
                        .in-app-message .close-icon::before,
                        .in-app-message .close-icon::after {
                            background-color: #000000;
                        }

                    </style>
                    <div class="close-icon" data-actiontype="close"></div>

                    <div class="content">
                        <span class="title" style="color:#000000;font-size:22px">
                            Book a tour for Antelope Canyon
                        </span>
                        <span class="body" style="color:#000000;font-size:14px">
                            This is an example of your in-app message body text.
                        </span>
                        <div class="buttons">

                            <span class="button" style="color:#ffffff;background-color:#f44cac" data-link="message:%3C3358921718340173851@unknownmsgid%3E">
                                Deeplink
                            </span>
                            <span class="button" style="color:#ffffff;background-color:#f44cac" data-link="https://exponea.com">
                                Web
                            </span>

                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
        }
    }

    private lateinit var appInboxManager: AppInboxManager
    private lateinit var appInboxCache: AppInboxCache
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var drawableCache: DrawableCache
    private lateinit var fetchManager: FetchManager
    private lateinit var apiService: ExponeaService

    @Before
    fun before() {
        fetchManager = mockk()
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4).invoke(Result(true, arrayListOf()))
        }
        drawableCache = mockk()
        every { drawableCache.has(any()) } returns true
        every { drawableCache.preload(any(), any()) } answers {
            arg<(Boolean) -> Unit>(1).invoke(true)
        }
        every { drawableCache.clear() } just Runs
        every { drawableCache.showImage(any(), any(), any()) } just Runs
        every { drawableCache.getFile(any()) } returns MockFile()
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds().apply {
            this.cookie = null
            this.externalIds = hashMapOf()
        }
        apiService = ExponeaMockService(true)
        appInboxCache = AppInboxCacheImpl(
            context = ApplicationProvider.getApplicationContext(),
            gson = Gson(),
            applicationId = "default-application"
        )
        // Need to be initialized to use bitmapCache for HTML parser
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialProject = ExponeaProject("https://base-url.com", "project-token", "Token auth")
        Exponea.flushMode = FlushMode.MANUAL
        skipInstallEvent()
        val configuration = ExponeaConfiguration(
            baseURL = initialProject.baseUrl,
            projectToken = initialProject.projectToken,
            authorization = initialProject.authorization
        )
        Exponea.init(context, configuration)
        appInboxManager = AppInboxManagerImpl(
            fetchManager = fetchManager,
            drawableCache = drawableCache,
            projectFactory = Exponea.componentForTesting.projectFactory,
            customerIdsRepository = customerIdsRepository,
            appInboxCache = appInboxCache,
            applicationId = Constants.ApplicationId.APP_ID_DEFAULT_VALUE
        )
    }

    @Test
    fun `should load only supported messages`() = runInSingleThread { idleThreads ->
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "push"),
                buildMessage("id2", type = "html"),
                buildMessage("id3", type = "whatSoEver")
            )))
        }
        var fetchedData: List<MessageItem>? = null
        appInboxManager.fetchAppInbox { data ->
            fetchedData = data
        }
        idleThreads()
        assertEquals(2, fetchedData?.size)
    }

    @Test
    fun `should track telemetry on fetch`() = runInSingleThread { idleThreads ->
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
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "push"),
                buildMessage("id2", type = "html"),
                buildMessage("id3", type = "whatSoEver")
            )))
        }
        appInboxManager.fetchAppInbox {}
        idleThreads()
        assertTrue(telemetryTelemetryEventSlot.isCaptured)
        val capturedEventType = telemetryTelemetryEventSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.APP_INBOX_INIT_FETCH, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertEquals("3", capturedProps["count"])
        assertTrue(capturedProps["data"]!!.isNotEmpty())
    }

    @Test
    fun `should parse PUSH message`() = runInSingleThread { idleThreads ->
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "push", data = mapOf(
                    "title" to "Title",
                    "message" to "Message",
                    "actions" to arrayOf(
                        buildActionAsMap(ExponeaNotificationActionType.BROWSER, "https://google.com", "Web"),
                        buildActionAsMap(ExponeaNotificationActionType.DEEPLINK, "mail:something", "Deeplink")
                    )
                ))
            )))
        }
        var fetchedData: List<MessageItem>? = null
        appInboxManager.fetchAppInbox { data ->
            fetchedData = data
        }
        idleThreads()
        assertEquals(1, fetchedData?.size)
        val pushMessage = fetchedData?.get(0)?.content
        assertNotNull(pushMessage)
        assertEquals("Title", pushMessage.title)
        assertEquals("Message", pushMessage.message)
        assertEquals(2, pushMessage.actions.size)
        val webAction = pushMessage.actions.find {
                act -> act.type.value == ExponeaNotificationActionType.BROWSER.value
        }
        assertNotNull(webAction)
        assertEquals("https://google.com", webAction.url)
        assertEquals("Web", webAction.title)
        val deeplinkAction = pushMessage.actions.find {
                act -> act.type.value == ExponeaNotificationActionType.DEEPLINK.value
        }
        assertNotNull(deeplinkAction)
        assertEquals("mail:something", deeplinkAction.url)
        assertEquals("Deeplink", deeplinkAction.title)
    }

    private fun buildActionAsMap(
        type: ExponeaNotificationActionType,
        url: String,
        title: String
    ) = mapOf(
        "action" to type.value,
        "url" to url,
        "title" to title
    )

    @Test
    fun `should parse HTML message`() = runInSingleThread { idleThreads ->
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "html", data = mapOf(
                    "title" to "Title",
                    "pre_header" to "Message",
                    "message" to buildHtmlMessageContent()
                ))
            )))
        }
        var fetchedData: List<MessageItem>? = null
        appInboxManager.fetchAppInbox { data ->
            fetchedData = data
        }
        idleThreads()
        assertEquals(1, fetchedData?.size)
        var pushMessage = fetchedData?.get(0)?.content
        assertNotNull(pushMessage)
        assertEquals("Title", pushMessage.title)
        assertEquals("Message", pushMessage.message)
        assertEquals(3, pushMessage.actions.size)
        var webAction = pushMessage.actions.find {
                act -> act.type.value == ExponeaNotificationActionType.BROWSER.value
        }
        assertNotNull(webAction)
        assertEquals("https://exponea.com", webAction.url)
        assertEquals("Web", webAction.title)
        var deeplinkAction = pushMessage.actions.find {
                act -> act.type.value == ExponeaNotificationActionType.DEEPLINK.value
        }
        assertNotNull(deeplinkAction)
        assertEquals("message:%3C3358921718340173851@unknownmsgid%3E", deeplinkAction.url)
        assertEquals("Deeplink", deeplinkAction.title)
        appInboxManager.fetchAppInbox { data ->
            fetchedData = data
        }
        idleThreads()
        assertEquals(1, fetchedData?.size)
        pushMessage = fetchedData?.get(0)?.content
        assertNotNull(pushMessage)
        assertEquals("Title", pushMessage.title)
        assertEquals("Message", pushMessage.message)
        assertEquals(3, pushMessage.actions.size)
        webAction = pushMessage.actions.find { act ->
            act.type.value == MessageItemAction.Type.BROWSER.value
        }
        assertNotNull(webAction)
        assertEquals("https://exponea.com", webAction.url)
        assertEquals("Web", webAction.title)
        deeplinkAction = pushMessage.actions.find { act ->
            act.type.value == MessageItemAction.Type.DEEPLINK.value
        }
        assertNotNull(deeplinkAction)
        assertEquals("message:%3C3358921718340173851@unknownmsgid%3E", deeplinkAction.url)
        assertEquals("Deeplink", deeplinkAction.title)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should deny markAsRead action for empty AppInbox`() {
        // fetchManager should not be called but keep it
        val testMessage = buildMessage("id1", type = "push")
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            testMessage,
                            buildMessage("id2", type = "html")
                        ),
                        "sync_123"
                    )
                )
        }
        waitForIt { done ->
            appInboxManager.markMessageAsRead(testMessage) { marked ->
                assertFalse(marked)
                done()
            }
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should allow markAsRead action after fetched AppInbox`() = runInSingleThread { idleThreads ->
        skipInstallEvent()
        identifyCustomer(cookie = "hash-cookie")
        val testMessage = buildMessage("id1", type = "push")
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            testMessage,
                            buildMessage("id2", type = "html")
                        ),
                        "sync_123"
                    )
                )
        }
        // fetchManager should not be asked for marking so fail if so
        every { fetchManager.markAppInboxAsRead(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<FetchError>) -> Unit>(5)
                .invoke(Result(false, FetchError(null, "Should not be called")))
        }
        appInboxManager.markMessageAsRead(testMessage) { marked ->
            assertFalse(marked)
        }
        idleThreads()
        var fetchedData: List<MessageItem>? = null
        appInboxManager.fetchAppInbox { data ->
            fetchedData = data
        }
        idleThreads()
        assertEquals(2, fetchedData?.size)
        // fetchManager should be asked for marking so valid answer is expected
        every { fetchManager.markAppInboxAsRead(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<Any?>) -> Unit>(4)
                .invoke(Result(true, null))
        }
        var markedResult: Boolean = false
        appInboxManager.markMessageAsRead(testMessage) { marked ->
            markedResult = marked
        }
        idleThreads()
        assertTrue(markedResult)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call markAsRead action with same customerIds as fetched AppInbox - onlyCookie`() = runInSingleThread {
        // setup
        identifyCustomer(cookie = "hash-cookie")
        val customerIdsWhileFetch = slot<CustomerIds>()
        val customerIdsWhileMarkAsRead = slot<CustomerIds>()
        runFetchAndMarkAsRead(customerIdsWhileFetch, customerIdsWhileMarkAsRead)
        assertTrue(customerIdsWhileFetch.isCaptured)
        assertTrue(customerIdsWhileMarkAsRead.isCaptured)
        val customerIdsMapWhileFetch = customerIdsWhileFetch.captured.toHashMap().filter { it.value != null }
        val customerIdsMapWhileRead = customerIdsWhileMarkAsRead.captured.toHashMap().filter { it.value != null }
        assertEquals(1, customerIdsMapWhileFetch.size)
        assertEquals(1, customerIdsMapWhileRead.size)
        assertEquals(customerIdsMapWhileFetch, customerIdsMapWhileRead)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call markAsRead action with same customerIds as fetched AppInbox - singleExternal`() =
        runInSingleThread {
            // setup
            identifyCustomer(ids = hashMapOf("registered" to "email@test.com"))
            val customerIdsWhileFetch = slot<CustomerIds>()
            val customerIdsWhileMarkAsRead = slot<CustomerIds>()
            runFetchAndMarkAsRead(customerIdsWhileFetch, customerIdsWhileMarkAsRead)
            assertTrue(customerIdsWhileFetch.isCaptured)
            assertTrue(customerIdsWhileMarkAsRead.isCaptured)
            val customerIdsMapWhileFetch = customerIdsWhileFetch.captured.toHashMap().filter { it.value != null }
            val customerIdsMapWhileRead = customerIdsWhileMarkAsRead.captured.toHashMap().filter { it.value != null }
            assertEquals(1, customerIdsMapWhileFetch.size)
            assertEquals(1, customerIdsMapWhileRead.size)
            assertEquals(customerIdsMapWhileFetch, customerIdsMapWhileRead)
        }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call markAsRead action with same customerIds as fetched AppInbox - multipleExternal`() =
        runInSingleThread {
            // setup
            identifyCustomer(
                ids = hashMapOf(
                    "registered" to "email@test.com",
                    "registered2" to "email2@test.com",
                    "registered3" to "email3@test.com"
                )
            )
            val customerIdsWhileFetch = slot<CustomerIds>()
            val customerIdsWhileMarkAsRead = slot<CustomerIds>()
            runFetchAndMarkAsRead(customerIdsWhileFetch, customerIdsWhileMarkAsRead)
            assertTrue(customerIdsWhileFetch.isCaptured)
            assertTrue(customerIdsWhileMarkAsRead.isCaptured)
            val customerIdsMapWhileFetch = customerIdsWhileFetch.captured.toHashMap().filter { it.value != null }
            val customerIdsMapWhileRead = customerIdsWhileMarkAsRead.captured.toHashMap().filter { it.value != null }
            assertEquals(3, customerIdsMapWhileFetch.size)
            assertEquals(3, customerIdsMapWhileRead.size)
            assertEquals(customerIdsMapWhileFetch, customerIdsMapWhileRead)
        }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call markAsRead action with same customerIds as fetched AppInbox - cookieAndExternal`() =
        runInSingleThread {
            // setup
            identifyCustomer(
                cookie = "hash-cookie", ids = hashMapOf(
                    "registered" to "email@test.com",
                    "registered2" to "email2@test.com",
                    "registered3" to "email3@test.com"
                )
            )
            val customerIdsWhileFetch = slot<CustomerIds>()
            val customerIdsWhileMarkAsRead = slot<CustomerIds>()
            runFetchAndMarkAsRead(customerIdsWhileFetch, customerIdsWhileMarkAsRead)
            assertTrue(customerIdsWhileFetch.isCaptured)
            assertTrue(customerIdsWhileMarkAsRead.isCaptured)
            val customerIdsMapWhileFetch = customerIdsWhileFetch.captured.toHashMap().filter { it.value != null }
            val customerIdsMapWhileRead = customerIdsWhileMarkAsRead.captured.toHashMap().filter { it.value != null }
            assertEquals(4, customerIdsMapWhileFetch.size)
            assertEquals(4, customerIdsMapWhileRead.size)
            assertEquals(customerIdsMapWhileFetch, customerIdsMapWhileRead)
        }

    @Test
    fun `should return actual messages for customer`() {
        val awaitSeconds = 5L
        val untilCustomerChanged = CountDownLatch(1)
        val untilFirstFetchStarted = CountDownLatch(1)
        val untilFetchProcessIsDone = CountDownLatch(1)
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            val customerIdsUsedForFetch = arg<CustomerIds>(1).toHashMap()["registered"]
            untilFirstFetchStarted.countDown()
            assertTrue(untilCustomerChanged.await(awaitSeconds, TimeUnit.SECONDS))
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1", type = "html", data = mapOf(
                                "title" to "Assigned to $customerIdsUsedForFetch",
                                "pre_header" to "Message",
                                "message" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        // step 1: be customer 'user1'
        identifyCustomer(cookie = "cookie1", ids = hashMapOf("registered" to "user1"))
        // step 2: invoke fetching while customer is 'user1'
        var fetchedAppInboxData: List<MessageItem>? = null
        appInboxManager.fetchAppInbox {
            fetchedAppInboxData = it
            untilFetchProcessIsDone.countDown()
        }
        // step 3: change to customer 'user2' while first fetch is running
        assertTrue(untilFirstFetchStarted.await(awaitSeconds, TimeUnit.SECONDS))
        identifyCustomer(cookie = "cookie2", ids = hashMapOf("registered" to "user2"))
        untilCustomerChanged.countDown()
        // step 4: wait until second fetch is done
        val fetchDataReceived = untilFetchProcessIsDone.await(1, TimeUnit.SECONDS)
        if (!fetchDataReceived) {
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue(untilFetchProcessIsDone.await(awaitSeconds, TimeUnit.SECONDS))
        // validate:
        assertNotNull(fetchedAppInboxData)
        assertEquals(1, fetchedAppInboxData?.size)
        val message = fetchedAppInboxData?.get(0)?.content
        assertEquals("Assigned to user2", message?.title)
    }

    @Test
    fun `should return actual messages for customer for all fetch attempts`() {
        var fetchedAppInboxData1: List<MessageItem>? = null
        var fetchedAppInboxData2: List<MessageItem>? = null
        var fetchedAppInboxData3: List<MessageItem>? = null
        val awaitSeconds = 5L
        val releaseFetchProces = CountDownLatch(1)
        val untilFirstFetchStarted = CountDownLatch(1)
        val untilFetchProcessIsDone1 = CountDownLatch(1)
        val untilFetchProcessIsDone2 = CountDownLatch(1)
        val untilFetchProcessIsDone3 = CountDownLatch(1)
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any(), any()) } answers {
            val customerIdsUsedForFetch = arg<CustomerIds>(1).toHashMap()["registered"] ?: "not-registered"
            untilFirstFetchStarted.countDown()
            assertTrue(releaseFetchProces.await(awaitSeconds, TimeUnit.SECONDS))
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1", type = "html", data = mapOf(
                                "title" to "Assigned to $customerIdsUsedForFetch",
                                "pre_header" to "Message",
                                "message" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        appInboxManager.fetchAppInbox {
            fetchedAppInboxData1 = it
            untilFetchProcessIsDone1.countDown()
        }
        identifyCustomer(cookie = "cookie1", ids = hashMapOf("registered" to "user1"))
        appInboxManager.fetchAppInbox {
            fetchedAppInboxData2 = it
            untilFetchProcessIsDone2.countDown()
        }
        assertTrue(untilFirstFetchStarted.await(awaitSeconds, TimeUnit.SECONDS))
        identifyCustomer(cookie = "cookie2", ids = hashMapOf("registered" to "user2"))
        appInboxManager.fetchAppInbox {
            fetchedAppInboxData3 = it
            untilFetchProcessIsDone3.countDown()
        }
        releaseFetchProces.countDown()
        val fetchDataReceived = untilFetchProcessIsDone1.await(1, TimeUnit.SECONDS)
        if (!fetchDataReceived) {
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue(untilFetchProcessIsDone1.await(awaitSeconds, TimeUnit.SECONDS))
        for (fetchedData in listOf(fetchedAppInboxData1, fetchedAppInboxData2, fetchedAppInboxData3)) {
            assertNotNull(fetchedData)
            assertEquals(1, fetchedData.size)
            assertEquals("Assigned to user2", fetchedData[0].content?.title)
        }
    }

    private fun runFetchAndMarkAsRead(
        customerIdsWhileFetch: CapturingSlot<CustomerIds>,
        customerIdsWhileMarkAsRead: CapturingSlot<CustomerIds>
    ) {
        val testMessage = buildMessage("id1", type = "push")
        every {
            fetchManager.fetchAppInbox(any(), capture(customerIdsWhileFetch), any(), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(4)
                .invoke(
                    Result(
                        true,
                        arrayListOf(
                            testMessage,
                            buildMessage("id2", type = "html")
                        ),
                        "sync_123"
                    )
                )
        }
        // fetchManager should be asked for marking so valid answer is expected
        every {
            fetchManager.markAppInboxAsRead(any(), capture(customerIdsWhileMarkAsRead), any(), any(), any(), any())
        } answers {
            arg<(Result<Any?>) -> Unit>(4)
                .invoke(Result(true, null))
        }
        waitForIt { done ->
            appInboxManager.fetchAppInbox { data ->
                done()
            }
        }
        waitForIt { done ->
            appInboxManager.markMessageAsRead(testMessage) { marked ->
                done()
            }
        }
    }

    private fun identifyCustomer(cookie: String? = null, ids: HashMap<String, String?> = hashMapOf()) {
        every { customerIdsRepository.get() } returns CustomerIds().apply {
            this.cookie = cookie
            this.externalIds = ids
        }
        appInboxManager.onEventCreated(mockkClass(Event::class), EventType.TRACK_CUSTOMER)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should not allow markAsRead action if SDK is stopping`() {
        Exponea.isStopped = true
        val testMessage = buildMessage("id1", type = "push")
        waitForIt { done ->
            appInboxManager.markMessageAsRead(testMessage) { marked ->
                assertFalse(marked)
                done()
            }
        }
    }

    @Test
    fun `should not allow fetch if SDK is stopping`() = runInSingleThread { idleThreads ->
        Exponea.isStopped = true
        var fetchedData: List<MessageItem>? = listOf()
        appInboxManager.fetchAppInbox { data ->
            fetchedData = data
        }
        idleThreads()
        assertNull(fetchedData)
    }
}
