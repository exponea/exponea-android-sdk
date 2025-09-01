package com.exponea.sdk.manager

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DateFilter
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockActionType
import com.exponea.sdk.models.InAppContentBlockDisplayState
import com.exponea.sdk.models.InAppContentBlockFrequency
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.InAppContentBlockStatus
import com.exponea.sdk.models.InAppContentBlockType
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepository
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockDataLoader
import com.exponea.sdk.testutil.runInSingleThread
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockManagerImplTest {

    companion object {
        fun buildMessage(
            id: String = "12345",
            type: String? = null,
            data: Map<String, Any?>? = null,
            placeholders: List<String> = listOf("placeholder_1"),
            trackingConsentCategory: String? = null,
            priority: Int? = null,
            rawFrequency: String = InAppContentBlockFrequency.ALWAYS.name.lowercase(),
            dateFilter: DateFilter? = null,
            name: String = "Random name"
        ): InAppContentBlock {
            return InAppContentBlock(
                id = id,
                name = name,
                dateFilter = dateFilter,
                rawFrequency = rawFrequency,
                priority = priority,
                consentCategoryTracking = trackingConsentCategory,
                rawContentType = type,
                content = data,
                placeholders = placeholders
            )
        }

        fun buildMessageData(
            id: String,
            status: String? = InAppContentBlockStatus.OK.value,
            ttl: Int? = null,
            hasTrackingConsent: Boolean? = null,
            variantId: Int? = null,
            variantName: String? = null,
            type: String? = null,
            data: Map<String, Any?>? = null
        ): InAppContentBlockPersonalizedData {
            return InAppContentBlockPersonalizedData(
                blockId = id,
                rawStatus = status,
                timeToLive = ttl,
                rawHasTrackingConsent = hasTrackingConsent,
                variantId = variantId,
                variantName = variantName,
                rawContentType = type,
                content = data
            )
        }

        /**
         * Copied directly from APP editor, contains 2 actions (deeplink + browser), 1 image, title and message
         */
        fun buildHtmlMessageContent(): String {
            return """
            <html>
            <head>
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

            </style>
            </head>
            <body>
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
                            <span class="button" style="color:#ffffff;background-color:#f44cac" data-link="https://exponea.com?xnpe_force_track=true">
                                Web forced
                            </span>

                        </div>
                    </div>
                </div>
            </div>
            </body>
            </html>
        """.trimIndent()
        }

        fun buildAction(
            type: InAppContentBlockActionType = InAppContentBlockActionType.BROWSER,
            actionName: String = "InApp action",
            actionUrl: String = "www.example.com"
        ): InAppContentBlockAction {
            return InAppContentBlockAction(
                type = type,
                name = actionName,
                url = actionUrl
            )
        }
    }

    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var drawableCache: DrawableCache
    private lateinit var fetchManager: FetchManager
    private lateinit var displayStateRepository: InAppContentBlockDisplayStateRepository
    private lateinit var projectFactory: ExponeaProjectFactory
    private lateinit var htmlCache: HtmlNormalizedCache
    private lateinit var fontCache: FontCache
    private lateinit var inAppContentBlockManager: InAppContentBlockManager

    @Before
    fun before() {
        Exponea.telemetry = null
        fetchManager = mock()
        customerIdsRepository = mock()
        displayStateRepository = mock {
            on { get(any()) } doReturn InAppContentBlockDisplayState(
                null, 0, null, 0
            )
            doNothing().on { setDisplayed(any(), any()) }
            doNothing().on { setInteracted(any(), any()) }
            doNothing().on { clear() }
        }
        drawableCache = mock {
            on { has(any()) } doReturn false
            doNothing().on { preload(any(), any()) }
            doNothing().on { clear() }
        }
        fontCache = mock {
            on { has(any()) } doReturn false
            doNothing().on { preload(any(), any()) }
        }
        val configuration = ExponeaConfiguration(
            projectToken = "token",
            authorization = "Token auth",
            baseURL = "https://test.com"
        )
        projectFactory = ExponeaProjectFactory(ApplicationProvider.getApplicationContext(), configuration)
        htmlCache = mock {
            doNothing().on { remove(any()) }
            on { get(any(), any()) } doReturn null
            doNothing().on { set(any(), any(), any()) }
        }
        inAppContentBlockManager = InAppContentBlockManagerImpl(
            displayStateRepository = displayStateRepository,
            fetchManager = fetchManager,
            projectFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
            imageCache = drawableCache,
            htmlCache = htmlCache,
            fontCache = fontCache
        )
        identifyCustomer()
    }

    @After
    fun after() {
        Exponea.telemetry = null
        Exponea.isStopped = false
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should load only supported messages`() = runInSingleThread {
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage("id1", type = "native"),
                        buildMessage("id2", type = "html"),
                        buildMessage("id3", type = "whatSoEver")
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        assertEquals(1, (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData.size)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should not load messages if SDK is stopped`() = runInSingleThread {
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage("id1", type = "native"),
                        buildMessage("id2", type = "html"),
                        buildMessage("id3", type = "whatSoEver")
                    )
                )
            )
        }
        Exponea.isStopped = true
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        assertEquals(0, (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData.size)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should not load messages if SDK is stopped while fetching`() = runInSingleThread {
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            Exponea.isStopped = true
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage("id1", type = "native"),
                        buildMessage("id2", type = "html"),
                        buildMessage("id3", type = "whatSoEver")
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        assertEquals(0, (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData.size)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should parse HTML message in Static-InAppContentBlock`() = runInSingleThread {
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1", type = "html", data = mapOf(
                                "html" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val staticForm = (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData.firstOrNull()
        assertNotNull(staticForm)
        assertEquals("id1", staticForm.id)
        assertEquals(InAppContentBlockType.HTML, staticForm.contentType)
        assertNotNull(staticForm.htmlContent)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should update non-loaded Personalized InAppContentBlock with fresh content`() = runInSingleThread {
        val placeholderId = "ph1"
        val messageId = "id1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            messageId,
                            placeholders = listOf(placeholderId),
                            dateFilter = null
                        ) // static has not contentType nor data
                    )
                )
            )
        }
        whenever(
            fetchManager.fetchPersonalizedContentBlocks(
                any(),
                any(),
                argThat<List<String>> { list -> list.size == 1 && list[0] == messageId },
                any(),
                any()
            )
        ).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(
                Result(
                    true, arrayListOf(
                        // htmlContent
                        buildMessageData(
                            messageId,
                            type = "html",
                            hasTrackingConsent = false,
                            data = mapOf(
                                "html" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val staticForm = (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData
            .firstOrNull()
        assertNotNull(staticForm)
        assertEquals(messageId, staticForm.id)
        assertEquals(InAppContentBlockType.NOT_DEFINED, staticForm.contentType)
        assertNull(staticForm.htmlContent)
        (inAppContentBlockManager as InAppContentBlockManagerImpl).loadContent(placeholderId)
        val personalizedForm = (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData
            .firstOrNull()
        assertNotNull(personalizedForm)
        assertNotNull(personalizedForm.personalizedData)
        assertEquals(messageId, personalizedForm.id)
        assertEquals(messageId, personalizedForm.personalizedData!!.blockId)
        assertEquals(InAppContentBlockType.HTML, personalizedForm.contentType)
        assertNotNull(personalizedForm.htmlContent)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should not load Personalized InAppContentBlock if SDK is stopped`() = runInSingleThread {
        val placeholderId = "ph1"
        val messageId = "id1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            messageId,
                            placeholders = listOf(placeholderId),
                            dateFilter = null
                        ) // static has not contentType nor data
                    )
                )
            )
        }
        whenever(
            fetchManager.fetchPersonalizedContentBlocks(
                any(),
                any(),
                argThat<List<String>> { list -> list.size == 1 && list[0] == messageId },
                any(),
                any()
            )
        ).thenAnswer {
            Exponea.isStopped = true
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(
                Result(
                    true, arrayListOf(
                        // htmlContent
                        buildMessageData(
                            messageId,
                            type = "html",
                            hasTrackingConsent = false,
                            data = mapOf(
                                "html" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        val inAppContentBlockManagerImpl = inAppContentBlockManager as InAppContentBlockManagerImpl
        // static fetch is successful
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val staticForm = inAppContentBlockManagerImpl.contentBlocksData.firstOrNull()
        assertNotNull(staticForm)
        assertEquals(messageId, staticForm.id)
        assertEquals(InAppContentBlockType.NOT_DEFINED, staticForm.contentType)
        assertNull(staticForm.htmlContent)
        // but personalized fetch will fail due to stopped SDK
        inAppContentBlockManagerImpl.loadContent(placeholderId)
        val personalizedForm = inAppContentBlockManagerImpl.contentBlocksData.firstOrNull()
        assertNotNull(personalizedForm)
        assertNull(personalizedForm.personalizedData)
        assertEquals(messageId, personalizedForm.id)
        assertEquals(InAppContentBlockType.NOT_DEFINED, personalizedForm.contentType)
        assertNull(personalizedForm.htmlContent)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should refresh message only after expiration of ttl`() = runInSingleThread {
        val placeholderId = "ph1"
        val messageId = "id1"
        var personalizedLoadCount = 0
        val ttlSeconds = 5
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            messageId,
                            placeholders = listOf(placeholderId),
                            dateFilter = null
                        ) // static has not contentType nor data
                    )
                )
            )
        }
        whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(
                Result(
                    true, arrayListOf(
                        // htmlContent
                        buildMessageData(
                            messageId,
                            ttl = ttlSeconds,
                            type = "html",
                            hasTrackingConsent = false,
                            data = mapOf(
                                "html" to buildHtmlMessageContent(),
                                // out of data, but used to verify
                                "loadCount" to ++personalizedLoadCount
                            )
                        )
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        (inAppContentBlockManager as InAppContentBlockManagerImpl).loadContent(placeholderId)
        assertEquals(1, personalizedLoadCount)
        (inAppContentBlockManager as InAppContentBlockManagerImpl).loadContent(placeholderId)
        assertEquals(1, personalizedLoadCount)
        Thread.sleep((ttlSeconds + 1) * 1000L)
        (inAppContentBlockManager as InAppContentBlockManagerImpl).loadContent(placeholderId)
        assertEquals(2, personalizedLoadCount)
        val message = (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData.firstOrNull()
        assertNotNull(message)
        assertNotNull(message.personalizedData)
        assertNotNull(message.personalizedData!!.content)
        assertEquals(2, (message.personalizedData!!.content!!["loadCount"] as Number).toInt())
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should auto-load messages content by configuration`() = runInSingleThread {
        val placeholderToAutoLoad = "ph1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1",
                            placeholders = listOf(placeholderToAutoLoad, "ph2"),
                            dateFilter = null
                        ), // static has not contentType nor data
                        buildMessage(
                            "id2",
                            placeholders = listOf("ph3"),
                            dateFilter = null
                        ) // static has not contentType nor data
                    )
                )
            )
        }
        whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(
                Result(
                    true, arrayListOf(
                        // htmlContent
                        buildMessageData(
                            it.getArgument<List<String>>(2).first(),
                            type = "html",
                            hasTrackingConsent = false,
                            data = mapOf(
                                "html" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        // flag is read from ExponeaProject, need to be re-created
        val configuration = ExponeaConfiguration(
            projectToken = "token",
            authorization = "Token auth",
            baseURL = "https://test.com",
            inAppContentBlockPlaceholdersAutoLoad = listOf(placeholderToAutoLoad)
        )
        projectFactory = ExponeaProjectFactory(ApplicationProvider.getApplicationContext(), configuration)
        inAppContentBlockManager = InAppContentBlockManagerImpl(
            displayStateRepository = displayStateRepository,
            fetchManager = fetchManager,
            projectFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
            imageCache = drawableCache,
            htmlCache = htmlCache,
            fontCache = fontCache
        )
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val messages = (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData
        assertEquals(2, messages.size)
        val message1 = messages.find { it.id == "id1" }
        assertNotNull(message1)
        assertNotNull(message1.personalizedData)
        val message2 = messages.find { it.id == "id2" }
        assertNotNull(message2)
        assertNull(message2.personalizedData)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track event with original customer`() = runInSingleThread {
        val placeholderId = "ph1"
        val messageId = "id1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            messageId,
                            placeholders = listOf(placeholderId)
                        ) // static has not contentType nor data
                    )
                )
            )
        }
        whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(
                Result(
                    true, arrayListOf(
                        // htmlContent
                        buildMessageData(
                            messageId,
                            type = "html",
                            hasTrackingConsent = true,
                            data = mapOf(
                                "html" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        identifyCustomer(cookie = "brownie1", ids = hashMapOf("login" to "test"))
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val message = (inAppContentBlockManager as InAppContentBlockManagerImpl).loadContent(placeholderId)
        assertNotNull(message)
        assertEquals("brownie1", message.customerIds["cookie"])
        assertEquals("test", message.customerIds["login"])
        identifyCustomer(cookie = "brownie2", ids = hashMapOf("login" to "another"))
        assertNotNull(message)
        assertEquals("brownie1", message.customerIds["cookie"])
        assertEquals("test", message.customerIds["login"])
        val message2 = (inAppContentBlockManager as InAppContentBlockManagerImpl).loadContent(placeholderId)
        assertNotNull(message2)
        assertEquals(message.id, message2.id)
        assertEquals("brownie2", message2.customerIds["cookie"])
        assertEquals("another", message2.customerIds["login"])
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should remove personal data after customer login`() = runInSingleThread {
        val placeholderId = "ph1"
        val messageId = "id1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            messageId,
                            placeholders = listOf(placeholderId)
                        ) // static has not contentType nor data
                    )
                )
            )
        }
        whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(
                Result(
                    true, arrayListOf(
                        // htmlContent
                        buildMessageData(
                            messageId,
                            type = "html",
                            hasTrackingConsent = true,
                            data = mapOf(
                                "html" to buildHtmlMessageContent()
                            )
                        )
                    )
                )
            )
        }
        identifyCustomer(cookie = "brownie1", ids = hashMapOf("login" to "test"))
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val message = (inAppContentBlockManager as InAppContentBlockManagerImpl).loadContent(placeholderId)
        assertNotNull(message)
        assertNotNull(message.personalizedData)
        identifyCustomer(cookie = "brownie2", ids = hashMapOf("login" to "another"))
        // picked message hasn't to be intact
        assertNotNull(message)
        assertNotNull(message.personalizedData)
        val internalMessage = (inAppContentBlockManager as InAppContentBlockManagerImpl).contentBlocksData
            .firstOrNull()
        assertNotNull(internalMessage)
        assertEquals(message.id, internalMessage.id)
        assertNull(internalMessage.personalizedData)
    }

    private fun identifyCustomer(cookie: String? = null, ids: HashMap<String, String?> = hashMapOf()) {
        whenever(customerIdsRepository.get()) doReturn CustomerIds().apply {
            this.cookie = cookie
            this.externalIds = ids
        }
        inAppContentBlockManager.onEventCreated(Event(), EventType.TRACK_CUSTOMER)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should prioritize message by priority value`() = runInSingleThread {
        val placeholderId = "ph1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            id = "1",
                            placeholders = listOf(placeholderId),
                            priority = null,
                            dateFilter = null
                        ),
                        buildMessage(
                            id = "2",
                            placeholders = listOf(placeholderId),
                            priority = 0,
                            dateFilter = null
                        ),
                        buildMessage(
                            id = "3",
                            placeholders = listOf(placeholderId),
                            priority = 1000,
                            dateFilter = null
                        ),
                        buildMessage(
                            id = "4",
                            placeholders = listOf(placeholderId),
                            priority = 10,
                            dateFilter = null
                        )
                    )
                )
            )
        }
        whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())).thenAnswer {
            val msgData = arrayListOf<InAppContentBlockPersonalizedData>()
            it.getArgument<List<String>>(2).forEach { messageId ->
                msgData.add(
                    buildMessageData(
                        messageId,
                        type = "html",
                        hasTrackingConsent = false,
                        data = mapOf("html" to buildHtmlMessageContent())
                    )
                )
            }
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3)
                .invoke(Result(true, msgData))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        repeat((0..1000).count()) {
            val chosenContentBlock = (inAppContentBlockManager as InAppContentBlockManagerImpl)
                .loadContent(placeholderId)
            assertNotNull(chosenContentBlock)
            assertNotNull(chosenContentBlock.personalizedData)
            assertEquals("3", chosenContentBlock.id)
            assertEquals("3", chosenContentBlock.personalizedData!!.blockId)
            assertEquals(InAppContentBlockType.HTML, chosenContentBlock.contentType)
            assertNotNull(chosenContentBlock.htmlContent)
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should track show message into telemetry`() = runInSingleThread { idleThreads ->
        val contentBlock = buildMessage("id2", type = "html")
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        contentBlock
                    )
                )
            )
        }
        val telemetryTelemetryEventCaptor = argumentCaptor<com.exponea.sdk.telemetry.model.TelemetryEvent>()
        val telemetryPropertiesCaptor = argumentCaptor<MutableMap<String, String>>()

        Exponea.telemetry = mock {
            doNothing().on {
                reportEvent(
                    telemetryTelemetryEventCaptor.capture(),
                    telemetryPropertiesCaptor.capture()
                )
            }
        }
        inAppContentBlockManager.getPlaceholderView(
            "placeholder_1",
            object : InAppContentBlockDataLoader {
                override fun loadContent(placeholderId: String): InAppContentBlock? {
                    return contentBlock
                }
            },
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(defferedLoad = false)
        )
        idleThreads()

        val capturedEventType = telemetryTelemetryEventCaptor.lastValue
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.CONTENT_BLOCK_SHOWN, capturedEventType)

        val capturedProps = telemetryPropertiesCaptor.lastValue
        assertNotNull(capturedProps)
        assertEquals("static", capturedProps["type"])
    }

    @Test
    fun `should get placeholder with defined loader`() = runInSingleThread { idleThreads ->
        val contentBlock = buildMessage("id1", type = "html")
        var askedPlaceholderId: String? = null
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            "placeholder_id",
            object : InAppContentBlockDataLoader {
                override fun loadContent(placeholderId: String): InAppContentBlock {
                    askedPlaceholderId = placeholderId
                    return contentBlock
                }
            },
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
        )
        assertNotNull(placeholder)
        placeholder.refreshContent()
        idleThreads()
        assertEquals("placeholder_id", askedPlaceholderId)
        assertEquals("placeholder_id", placeholder.controller.placeholderId)
        assertEquals(contentBlock.id, placeholder.controller.assignedMessage?.id)
    }

    @Test
    fun `should pass for empty date filter`() {
        val contentBlock = buildMessage("id1", type = "html")
        assertTrue(inAppContentBlockManager.passesDateFilter(contentBlock))
    }

    @Test
    fun `should pass for disabled date filter`() {
        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        val contentBlock = buildMessage(
            "id1", type = "html", dateFilter = DateFilter(
                enabled = false,
                fromDate = nowSeconds + 10,
                toDate = nowSeconds + 20
            )
        )
        assertTrue(inAppContentBlockManager.passesDateFilter(contentBlock))
    }

    @Test
    fun `should not pass for date filter with missed time`() {
        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        val contentBlock = buildMessage(
            "id1", type = "html", dateFilter = DateFilter(
                enabled = true,
                fromDate = nowSeconds + 10,
                toDate = nowSeconds + 20
            )
        )
        assertFalse(inAppContentBlockManager.passesDateFilter(contentBlock))
    }

    @Test
    fun `should pass for frequency filter if show ALWAYS`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.ALWAYS.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(Date(), 1, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should pass for frequency filter if show ONLY_ONCE but not shown`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.ONLY_ONCE.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(null, 0, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should pass for frequency filter if show ONLY_ONCE but interacted`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.ONLY_ONCE.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(null, 0, Date(), 1))
            .whenever(displayStateRepository).get(contentBlock)
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should not pass for frequency filter if show ONLY_ONCE and already shown`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.ONLY_ONCE.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(Date(), 1, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertFalse(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should pass for frequency filter if show ONCE_PER_VISIT but not shown`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.ONCE_PER_VISIT.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(null, 0, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should pass for frequency filter if show ONCE_PER_VISIT but interacted`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.ONCE_PER_VISIT.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(null, 0, Date(), 1))
            .whenever(displayStateRepository).get(contentBlock)
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should not pass for frequency filter if show ONCE_PER_VISIT and already shown`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.ONCE_PER_VISIT.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(Date(), 1, null, 0))
            .whenever(displayStateRepository).get(contentBlock)

        assertFalse(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should pass for frequency filter if show UNTIL_VISITOR_INTERACTS but not interacted`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(null, 0, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should pass for frequency filter if show UNTIL_VISITOR_INTERACTS but only shown`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(Date(), 1, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should not pass for frequency filter if show UNTIL_VISITOR_INTERACTS and interacted`() {
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(null, 0, Date(), 1))
            .whenever(displayStateRepository).get(contentBlock)
        assertFalse(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
    }

    @Test
    fun `should pass filter if passed all inner filters`() {
        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            dateFilter = DateFilter(
                enabled = false,
                fromDate = nowSeconds + 10,
                toDate = nowSeconds + 20
            ),
            rawFrequency = InAppContentBlockFrequency.ALWAYS.name.lowercase()
        )
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
        assertTrue(inAppContentBlockManager.passesDateFilter(contentBlock))
        assertTrue(inAppContentBlockManager.passesFilters(contentBlock))
    }

    @Test
    fun `should not pass filter if not passed date filter`() {
        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            dateFilter = DateFilter(
                enabled = true,
                fromDate = nowSeconds + 10,
                toDate = nowSeconds + 20
            ),
            rawFrequency = InAppContentBlockFrequency.ALWAYS.name.lowercase()
        )
        assertTrue(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
        assertFalse(inAppContentBlockManager.passesDateFilter(contentBlock))
        assertFalse(inAppContentBlockManager.passesFilters(contentBlock))
    }

    @Test
    fun `should not pass filter if not passed frequency filter`() {
        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            dateFilter = DateFilter(
                enabled = false,
                fromDate = nowSeconds + 10,
                toDate = nowSeconds + 20
            ),
            rawFrequency = InAppContentBlockFrequency.ONLY_ONCE.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(Date(), 1, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertFalse(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
        assertTrue(inAppContentBlockManager.passesDateFilter(contentBlock))
        assertFalse(inAppContentBlockManager.passesFilters(contentBlock))
    }

    @Test
    fun `should not pass filter if none inner filters pass`() {
        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        val contentBlock = buildMessage(
            "id1",
            type = "html",
            dateFilter = DateFilter(
                enabled = true,
                fromDate = nowSeconds + 10,
                toDate = nowSeconds + 20
            ),
            rawFrequency = InAppContentBlockFrequency.ONLY_ONCE.name.lowercase()
        )
        doReturn(InAppContentBlockDisplayState(Date(), 1, null, 0))
            .whenever(displayStateRepository).get(contentBlock)
        assertFalse(inAppContentBlockManager.passesFrequencyFilter(contentBlock))
        assertFalse(inAppContentBlockManager.passesDateFilter(contentBlock))
        assertFalse(inAppContentBlockManager.passesFilters(contentBlock))
    }

    @Test
    fun `should return all content blocks but only for placeholder`() = runInSingleThread { idleThreads ->
        val placeholderId = "ph1"
        val nowSeconds = (System.currentTimeMillis() / 1000).toInt()
        doAnswer {
            if (it.getArgument<InAppContentBlock>(1).id == "invalidByFrequency") {
                InAppContentBlockDisplayState(
                    Date(), 1, null, 0
                )
            } else {
                InAppContentBlockDisplayState(
                    null, 0, null, 0
                )
            }
        }.whenever(displayStateRepository).get(any())
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            id = "invalidByDate",
                            placeholders = listOf(placeholderId),
                            dateFilter = DateFilter(
                                enabled = true,
                                fromDate = nowSeconds + 10,
                                toDate = nowSeconds + 20
                            ),
                            rawFrequency = InAppContentBlockFrequency.ALWAYS.name.lowercase()
                        ),
                        buildMessage(
                            id = "invalidByFrequency",
                            placeholders = listOf(placeholderId),
                            rawFrequency = InAppContentBlockFrequency.ONLY_ONCE.name.lowercase()
                        ),
                        buildMessage(
                            id = "valid",
                            placeholders = listOf(placeholderId),
                            priority = 1000
                        ),
                        buildMessage(
                            id = "invalidByPlaceholder",
                            placeholders = listOf("ph2"),
                            priority = 10
                        )
                    )
                )
            )
        }
        whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any())).thenAnswer {
            val msgData = arrayListOf<InAppContentBlockPersonalizedData>()
            it.getArgument<List<String>>(2).forEach { messageId ->
                msgData.add(
                    buildMessageData(
                        messageId,
                        type = "html",
                        hasTrackingConsent = false,
                        data = mapOf(
                            "html" to buildHtmlMessageContent()
                        )
                    )
                )
            }
            it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3)
                .invoke(Result(true, msgData))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val contentBlocks = inAppContentBlockManager.getAllInAppContentBlocksForPlaceholder(placeholderId = "ph1")
        assertEquals(3, contentBlocks.size)
        val loadedBlockIds = contentBlocks.map { it.id }
        assertTrue(loadedBlockIds.contains("invalidByDate"))
        assertTrue(loadedBlockIds.contains("invalidByFrequency"))
        assertTrue(loadedBlockIds.contains("valid"))
        assertFalse(loadedBlockIds.contains("invalidByPlaceholder"))
    }
}
