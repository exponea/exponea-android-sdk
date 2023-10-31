package com.exponea.sdk.manager

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockDisplayState
import com.exponea.sdk.models.InAppContentBlockFrequency
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppContentBlockStatus
import com.exponea.sdk.models.InAppContentBlockType
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.BitmapCache
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepository
import com.exponea.sdk.repository.SimpleFileCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlocksManagerImplTest {

    companion object {
        fun buildMessage(
            id: String,
            type: String? = null,
            data: Map<String, Any?>? = null,
            placeholders: List<String> = listOf("placeholder_1"),
            trackingConsentCategory: String? = null,
            priority: Int? = null
        ): InAppContentBlock {
            return InAppContentBlock(
                id = id,
                name = "Random name",
                dateFilter = null,
                rawFrequency = InAppContentBlockFrequency.ALWAYS.name.lowercase(),
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
                            <span class="button" style="color:#ffffff;background-color:#f44cac" data-link="https://exponea.com?xnpe_force_track=true">
                                Web forced
                            </span>

                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
        }
    }

    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var bitmapCache: BitmapCache
    private lateinit var fetchManager: FetchManager
    private lateinit var apiService: ExponeaService
    private lateinit var displayStateRepository: InAppContentBlockDisplayStateRepository
    private lateinit var projectFactory: ExponeaProjectFactory
    private lateinit var htmlCache: HtmlNormalizedCache
    private lateinit var fontCache: SimpleFileCache
    private lateinit var trackingConsentManager: TrackingConsentManager
    private lateinit var inAppContentBlockManager: InAppContentBlockManager

    @Before
    fun before() {
        fetchManager = mockk()
        customerIdsRepository = mockk()
        displayStateRepository = mockk()
        every { displayStateRepository.get(any()) } returns InAppContentBlockDisplayState(
            null, 0, null, 0
        )
        every { displayStateRepository.setDisplayed(any(), any()) } just Runs
        every { displayStateRepository.setInteracted(any(), any()) } just Runs
        every { displayStateRepository.clear() } just Runs
        bitmapCache = mockk()
        every { bitmapCache.has(any()) } returns false
        every { bitmapCache.preload(any(), any()) } just Runs
        every { bitmapCache.clearExcept(any()) } just Runs
        fontCache = mockk()
        every { fontCache.has(any()) } returns false
        every { fontCache.preload(any(), any()) } just Runs
        every { fontCache.clearExcept(any()) } just Runs
        val configuration = ExponeaConfiguration(
            projectToken = "token",
            authorization = "Token auth",
            baseURL = "https://test.com"
        )
        projectFactory = ExponeaProjectFactory(ApplicationProvider.getApplicationContext(), configuration)
        htmlCache = mockk()
        every { htmlCache.remove(any()) } just Runs
        every { htmlCache.get(any(), any()) } returns null
        every { htmlCache.set(any(), any(), any()) } just Runs
        trackingConsentManager = mockk()
        every { trackingConsentManager.trackInAppContentBlockClick(any(), any(), any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppContentBlockClose(any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppContentBlockShown(any(), any(), any()) } just Runs
        every { trackingConsentManager.trackInAppContentBlockError(any(), any(), any(), any()) } just Runs
        apiService = ExponeaMockService(true)
        inAppContentBlockManager = InAppContentBlocksManagerImpl(
            displayStateRepository = displayStateRepository,
            fetchManager = fetchManager,
            projectFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
            trackingConsentManager = trackingConsentManager,
            imageCache = bitmapCache,
            htmlCache = htmlCache,
            fontCache = fontCache
        )
        identifyCustomer()
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
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "native"),
                buildMessage("id2", type = "html"),
                buildMessage("id3", type = "whatSoEver")
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        assertEquals(1, (inAppContentBlockManager as InAppContentBlocksManagerImpl).contentBlocksData.size)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should parse HTML message in Static-InAppContentBlock`() {
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "html", data = mapOf(
                    "html" to buildHtmlMessageContent()
                ))
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val staticForm = (inAppContentBlockManager as InAppContentBlocksManagerImpl).contentBlocksData.firstOrNull()
        assertNotNull(staticForm)
        assertEquals("id1", staticForm.id)
        assertEquals(InAppContentBlockType.HTML, staticForm.contentType)
        assertNotNull(staticForm.htmlContent)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should update non-loaded Personalized InAppContentBlock with fresh content`() {
        val placeholderId = "ph1"
        val messageId = "id1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(
                    messageId,
                    placeholders = listOf(placeholderId)
                ) // static has not contentType nor data
            )))
        }
        every { fetchManager.fetchPersonalizedContentBlocks(any(), any(), listOf(messageId), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                // htmlContent
                buildMessageData(
                    messageId,
                    type = "html",
                    hasTrackingConsent = false,
                    data = mapOf(
                        "html" to buildHtmlMessageContent()
                    )
                )
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val staticForm = (inAppContentBlockManager as InAppContentBlocksManagerImpl).contentBlocksData
            .firstOrNull()
        assertNotNull(staticForm)
        assertEquals(messageId, staticForm.id)
        assertEquals(InAppContentBlockType.NOT_DEFINED, staticForm.contentType)
        assertNull(staticForm.htmlContent)
        (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
        val personalizedForm = (inAppContentBlockManager as InAppContentBlocksManagerImpl).contentBlocksData
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
    fun `should refresh message only after expiration of ttl`() {
        val placeholderId = "ph1"
        val messageId = "id1"
        var personalizedLoadCount: Int = 0
        val ttlSeconds = 5
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(
                    messageId,
                    placeholders = listOf(placeholderId)
                ) // static has not contentType nor data
            )))
        }
        every { fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
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
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
        assertEquals(1, personalizedLoadCount)
        (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
        assertEquals(1, personalizedLoadCount)
        Thread.sleep((ttlSeconds + 1) * 1000L)
        (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
        assertEquals(2, personalizedLoadCount)
        val message = (inAppContentBlockManager as InAppContentBlocksManagerImpl).contentBlocksData.firstOrNull()
        assertNotNull(message)
        assertNotNull(message.personalizedData)
        assertNotNull(message.personalizedData!!.content)
        assertEquals(2, (message.personalizedData!!.content!!.get("loadCount") as Double).toInt())
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should auto-load messages content by configuration`() {
        val placeholderToAutoLoad = "ph1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(
                    "id1",
                    placeholders = listOf(placeholderToAutoLoad, "ph2")
                ), // static has not contentType nor data
                buildMessage(
                    "id2",
                    placeholders = listOf("ph3")
                ) // static has not contentType nor data
            )))
        }
        every { fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                // htmlContent
                buildMessageData(
                    arg<List<String>>(2).first(),
                    type = "html",
                    hasTrackingConsent = false,
                    data = mapOf(
                        "html" to buildHtmlMessageContent()
                    )
                )
            )))
        }
        // flag is read from ExponeaProject, need to be re-created
        val configuration = ExponeaConfiguration(
            projectToken = "token",
            authorization = "Token auth",
            baseURL = "https://test.com",
            inAppContentBlockPlaceholdersAutoLoad = listOf(placeholderToAutoLoad)
        )
        projectFactory = ExponeaProjectFactory(ApplicationProvider.getApplicationContext(), configuration)
        inAppContentBlockManager = InAppContentBlocksManagerImpl(
            displayStateRepository = displayStateRepository,
            fetchManager = fetchManager,
            projectFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
            trackingConsentManager = trackingConsentManager,
            imageCache = bitmapCache,
            htmlCache = htmlCache,
            fontCache = fontCache
        )
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val messages = (inAppContentBlockManager as InAppContentBlocksManagerImpl).contentBlocksData
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
    fun `should track event with original customer`() {
        val placeholderId = "ph1"
        val messageId = "id1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(messageId, placeholders = listOf(placeholderId)) // static has not contentType nor data
            )))
        }
        every { fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                // htmlContent
                buildMessageData(
                    messageId,
                    type = "html",
                    hasTrackingConsent = true,
                    data = mapOf(
                        "html" to buildHtmlMessageContent()
                    )
                )
            )))
        }
        identifyCustomer(cookie = "brownie1", ids = hashMapOf("login" to "test"))
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val message = (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
        assertNotNull(message)
        assertEquals("brownie1", message.customerIds.get("cookie"))
        assertEquals("test", message.customerIds.get("login"))
        identifyCustomer(cookie = "brownie2", ids = hashMapOf("login" to "another"))
        assertNotNull(message)
        assertEquals("brownie1", message.customerIds.get("cookie"))
        assertEquals("test", message.customerIds.get("login"))
        val message2 = (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
        assertNotNull(message2)
        assertEquals(message.id, message2.id)
        assertEquals("brownie2", message2.customerIds.get("cookie"))
        assertEquals("another", message2.customerIds.get("login"))
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should remove personal data after customer login`() {
        val placeholderId = "ph1"
        val messageId = "id1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(messageId, placeholders = listOf(placeholderId)) // static has not contentType nor data
            )))
        }
        every { fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, arrayListOf(
                // htmlContent
                buildMessageData(
                    messageId,
                    type = "html",
                    hasTrackingConsent = true,
                    data = mapOf(
                        "html" to buildHtmlMessageContent()
                    )
                )
            )))
        }
        identifyCustomer(cookie = "brownie1", ids = hashMapOf("login" to "test"))
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val message = (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
        assertNotNull(message)
        assertNotNull(message.personalizedData)
        identifyCustomer(cookie = "brownie2", ids = hashMapOf("login" to "another"))
        // picked message hasn't to be intact
        assertNotNull(message)
        assertNotNull(message.personalizedData)
        val internalMessage = (inAppContentBlockManager as InAppContentBlocksManagerImpl).contentBlocksData
            .firstOrNull()
        assertNotNull(internalMessage)
        assertEquals(message.id, internalMessage.id)
        assertNull(internalMessage.personalizedData)
    }

    private fun identifyCustomer(cookie: String? = null, ids: HashMap<String, String?> = hashMapOf()) {
        every { customerIdsRepository.get() } returns CustomerIds().apply {
            this.cookie = cookie
            this.externalIds = ids
        }
        inAppContentBlockManager.onEventCreated(Event(), EventType.TRACK_CUSTOMER)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should prioritize message by priority value`() {
        val placeholderId = "ph1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                    buildMessage(
                            id = "1",
                            placeholders = listOf(placeholderId),
                            priority = null
                    ),
                    buildMessage(
                            id = "2",
                            placeholders = listOf(placeholderId),
                            priority = 0
                    ),
                    buildMessage(
                            id = "3",
                            placeholders = listOf(placeholderId),
                            priority = 1000
                    ),
                    buildMessage(
                            id = "4",
                            placeholders = listOf(placeholderId),
                            priority = 10
                    )
            )))
        }
        every { fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), any(), any()) } answers {
            val msgData = arrayListOf<InAppContentBlockPersonalizedData>()
            arg<List<String>>(2).forEach { messageId ->
                msgData.add(buildMessageData(
                        messageId,
                        type = "html",
                        hasTrackingConsent = false,
                        data = mapOf(
                                "html" to buildHtmlMessageContent()
                        )
                ))
            }
            arg<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(3).invoke(Result(true, msgData))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        for (i in 0..1000) {    // 1000 repeats are statistically minimum
            val chosenContentBlock = (inAppContentBlockManager as InAppContentBlocksManagerImpl).loadContent(placeholderId)
            assertNotNull(chosenContentBlock)
            assertNotNull(chosenContentBlock.personalizedData)
            assertEquals("3", chosenContentBlock.id)
            assertEquals("3", chosenContentBlock.personalizedData!!.blockId)
            assertEquals(InAppContentBlockType.HTML, chosenContentBlock.contentType)
            assertNotNull(chosenContentBlock.htmlContent)
        }
    }
}
