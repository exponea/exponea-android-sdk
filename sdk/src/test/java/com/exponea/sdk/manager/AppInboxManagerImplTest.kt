package com.exponea.sdk.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.NotificationPayload.Actions
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.AppInboxCache
import com.exponea.sdk.repository.AppInboxCacheImpl
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import com.google.gson.Gson
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        drawableCache = mockk()
        customerIdsRepository = mockk()
        every { drawableCache.has(any()) } returns false
        every { drawableCache.preload(any(), any()) } just Runs
        every { drawableCache.clearExcept(any()) } just Runs
        identifyCustomer()
        apiService = ExponeaMockService(true)
        appInboxCache = AppInboxCacheImpl(
            ApplicationProvider.getApplicationContext(), Gson()
        )
        // Need to be initialized to use bitmapCache for HTML parser
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialProject = ExponeaProject("https://base-url.com", "project-token", "Token auth")
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(
            baseURL = initialProject.baseUrl,
            projectToken = initialProject.projectToken,
            authorization = initialProject.authorization)
        )
        appInboxManager = AppInboxManagerImpl(
            fetchManager = fetchManager,
            drawableCache = drawableCache,
            projectFactory = Exponea.componentForTesting.projectFactory,
            customerIdsRepository = customerIdsRepository,
            appInboxCache = appInboxCache
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
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should load only supported messages`() {
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "push"),
                buildMessage("id2", type = "html"),
                buildMessage("id3", type = "whatSoEver")
            )))
        }
        waitForIt(20000) {
            appInboxManager.fetchAppInbox { data ->
                assertEquals(2, data?.size)
                it()
            }
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should parse PUSH message`() {
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "push", data = mapOf(
                    "title" to "Title",
                    "message" to "Message",
                    "actions" to arrayOf(
                        buildActionAsMap(Actions.BROWSER, "https://google.com", "Web"),
                        buildActionAsMap(Actions.DEEPLINK, "mail:something", "Deeplink")
                    )
                ))
            )))
        }
        waitForIt(20000) {
            appInboxManager.fetchAppInbox { data ->
                assertEquals(1, data?.size)
                val pushMessage = data?.get(0)?.content
                assertNotNull(pushMessage)
                assertEquals("Title", pushMessage.title)
                assertEquals("Message", pushMessage.message)
                assertEquals(2, pushMessage.actions.size)
                val webAction = pushMessage.actions.find { act -> act.type.value == Actions.BROWSER.value }
                assertNotNull(webAction)
                assertEquals("https://google.com", webAction.url)
                assertEquals("Web", webAction.title)
                val deeplinkAction = pushMessage.actions.find { act -> act.type.value == Actions.DEEPLINK.value }
                assertNotNull(deeplinkAction)
                assertEquals("mail:something", deeplinkAction.url)
                assertEquals("Deeplink", deeplinkAction.title)
                it()
            }
        }
    }

    private fun buildActionAsMap(
        type: Actions,
        url: String,
        title: String
    ) = mapOf(
        "action" to type.value,
        "url" to url,
        "title" to title
    )

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should parse HTML message`() {
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "html", data = mapOf(
                    "title" to "Title",
                    "pre_header" to "Message",
                    "message" to buildHtmlMessageContent()
                ))
            )))
        }
        appInboxManager.fetchAppInbox { data ->
            assertEquals(1, data?.size)
            val pushMessage = data?.get(0)?.content
            assertNotNull(pushMessage)
            assertEquals("Title", pushMessage.title)
            assertEquals("Message", pushMessage.message)
            assertEquals(2, pushMessage.actions.size)
            val webAction = pushMessage.actions.find { act -> act.type.value == Actions.BROWSER.value }
            assertNotNull(webAction)
            assertEquals("https://exponea.com", webAction.url)
            assertEquals("Web", webAction.title)
            val deeplinkAction = pushMessage.actions.find { act -> act.type.value == Actions.DEEPLINK.value }
            assertNotNull(deeplinkAction)
            assertEquals("message:%3C3358921718340173851@unknownmsgid%3E", deeplinkAction.url)
            assertEquals("Deeplink", deeplinkAction.title)
        }
//        waitForIt(200000) {
//            appInboxManager.fetchAppInbox { data ->
//                assertEquals(1, data?.size)
//                val pushMessage = data?.get(0)?.content
//                assertNotNull(pushMessage)
//                assertEquals("Title", pushMessage.title)
//                assertEquals("Message", pushMessage.message)
//                assertEquals(2, pushMessage.actions.size)
//                val webAction = pushMessage.actions.find { act -> act.type.value == Actions.BROWSER.value }
//                assertNotNull(webAction)
//                assertEquals("https://exponea.com", webAction.url)
//                assertEquals("Web", webAction.title)
//                val deeplinkAction = pushMessage.actions.find { act -> act.type.value == Actions.DEEPLINK.value }
//                assertNotNull(deeplinkAction)
//                assertEquals("message:%3C3358921718340173851@unknownmsgid%3E", deeplinkAction.url)
//                assertEquals("Deeplink", deeplinkAction.title)
//                it()
//            }
//        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should deny markAsRead action for empty AppInbox`() {
        // fetchManager should not be called but keep it
        val testMessage = buildMessage("id1", type = "push")
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(3)
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
        waitForIt(20000) { done ->
            appInboxManager.markMessageAsRead(testMessage) { marked ->
                assertFalse(marked)
                done()
            }
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should allow markAsRead action after fetched AppInbox`() {
        skipInstallEvent()
        identifyCustomer(cookie = "hash-cookie")
        val testMessage = buildMessage("id1", type = "push")
        every { fetchManager.fetchAppInbox(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(3)
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
        waitForIt(20000) {
            appInboxManager.markMessageAsRead(testMessage) { marked ->
                assertFalse(marked)
                it.invoke()
            }
        }
        waitForIt(20000) {
            appInboxManager.fetchAppInbox { data ->
                assertEquals(2, data?.size)
                it()
            }
        }
        // fetchManager should be asked for marking so valid answer is expected
        every { fetchManager.markAppInboxAsRead(any(), any(), any(), any(), any(), any()) } answers {
            arg<(Result<Any?>) -> Unit>(4)
                .invoke(Result(true, null))
        }
        waitForIt(20000) {
            appInboxManager.markMessageAsRead(testMessage) { marked ->
                assertTrue(marked)
                it()
            }
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call markAsRead action with same customerIds as fetched AppInbox - onlyCookie`() {
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
    fun `should call markAsRead action with same customerIds as fetched AppInbox - singleExternal`() {
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
    fun `should call markAsRead action with same customerIds as fetched AppInbox - multipleExternal`() {
        // setup
        identifyCustomer(ids = hashMapOf(
            "registered" to "email@test.com",
            "registered2" to "email2@test.com",
            "registered3" to "email3@test.com"
        ))
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
    fun `should call markAsRead action with same customerIds as fetched AppInbox - cookieAndExternal`() {
        // setup
        identifyCustomer(cookie = "hash-cookie", ids = hashMapOf(
            "registered" to "email@test.com",
            "registered2" to "email2@test.com",
            "registered3" to "email3@test.com"
        ))
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

    private fun runFetchAndMarkAsRead(
        customerIdsWhileFetch: CapturingSlot<CustomerIds>,
        customerIdsWhileMarkAsRead: CapturingSlot<CustomerIds>
    ) {
        val testMessage = buildMessage("id1", type = "push")
        every {
            fetchManager.fetchAppInbox(any(), capture(customerIdsWhileFetch), any(), any(), any())
        } answers {
            arg<(Result<ArrayList<MessageItem>?>) -> Unit>(3)
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
        waitForIt(20000) {
            appInboxManager.fetchAppInbox { data ->
                it()
            }
        }
        waitForIt(20000) {
            appInboxManager.markMessageAsRead(testMessage) { marked ->
                it()
            }
        }
    }

    private fun identifyCustomer(cookie: String? = null, ids: HashMap<String, String?> = hashMapOf()) {
        every { customerIdsRepository.get() } returns CustomerIds().apply {
            this.cookie = cookie
            this.externalIds = ids
        }
    }
}
