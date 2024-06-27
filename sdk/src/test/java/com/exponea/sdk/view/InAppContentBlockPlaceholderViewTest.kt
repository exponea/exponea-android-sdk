package com.exponea.sdk.view

import android.content.Context
import android.os.Looper.getMainLooper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.FetchManager
import com.exponea.sdk.manager.InAppContentBlockManager
import com.exponea.sdk.manager.InAppContentBlockManagerImpl
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildHtmlMessageContent
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildMessage
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockDisplayState
import com.exponea.sdk.models.InAppContentBlockFrequency
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepository
import com.exponea.sdk.repository.SimpleFileCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockPlaceholderViewTest {

    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var drawableCache: DrawableCache
    private lateinit var fetchManager: FetchManager
    private lateinit var apiService: ExponeaService
    private lateinit var displayStateRepository: InAppContentBlockDisplayStateRepository
    private lateinit var projectFactory: ExponeaProjectFactory
    private lateinit var htmlCache: HtmlNormalizedCache
    private lateinit var fontCache: SimpleFileCache
    private lateinit var inAppContentBlockManager: InAppContentBlockManager

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fetchManager = mockk()
        customerIdsRepository = mockk()
        displayStateRepository = InAppContentBlockDisplayStateMock()
        drawableCache = mockk()
        every { drawableCache.has(any()) } returns false
        every { drawableCache.preload(any(), any()) } just Runs
        every { drawableCache.clearExcept(any()) } just Runs
        every { drawableCache.getFile(any()) } returns MockFile()
        fontCache = mockk()
        every { fontCache.has(any()) } returns false
        every { fontCache.preload(any(), any()) } just Runs
        every { fontCache.clearExcept(any()) } just Runs
        val configuration = ExponeaConfiguration(
            projectToken = "token",
            authorization = "Token auth",
            baseURL = "https://test.com"
        )
        projectFactory = ExponeaProjectFactory(context, configuration)
        htmlCache = mockk()
        every { htmlCache.remove(any()) } just Runs
        every { htmlCache.get(any(), any()) } returns null
        every { htmlCache.set(any(), any(), any()) } just Runs
        apiService = ExponeaMockService(true)
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
    fun `should load message assigned to placeholder ID`() {
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val placeholder = inAppContentBlockManager.getPlaceholderView(
                "placeholder_1",
                ApplicationProvider.getApplicationContext(),
                InAppContentBlockPlaceholderConfiguration(true)
        )
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                assertEquals("id1", contentBlock.id)
            }
            override fun onNoMessageFound(placeholderId: String) {
                fail("Has to load message")
            }
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                fail("Should not throw error")
            }
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
            }
            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
            }
        }
        placeholder.refreshContent()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should call message changed events in correct order`() {
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            "placeholder_1",
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        preloadInAppContentBlocks(arrayListOf(
            buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        ))
        var messageFound = false
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                messageFound = true
            }
            override fun onNoMessageFound(placeholderId: String) {
                messageFound = false
            }
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
            }
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
            }
            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
            }
        }
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertTrue(messageFound)
        assertEquals(View.VISIBLE, placeholder.htmlContainer.visibility)
        assertEquals(View.GONE, placeholder.placeholder.visibility)
        preloadInAppContentBlocks(arrayListOf())
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertFalse(messageFound)
        assertEquals(View.GONE, placeholder.htmlContainer.visibility)
        assertEquals(View.VISIBLE, placeholder.placeholder.visibility)
        preloadInAppContentBlocks(arrayListOf(
            buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        ))
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertTrue(messageFound)
        assertEquals(View.VISIBLE, placeholder.htmlContainer.visibility)
        assertEquals(View.GONE, placeholder.placeholder.visibility)
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should store interaction flags by invoking manual action`() {
        val placeholderId = "ph1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent()),
                    placeholders = listOf(placeholderId),
                    rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase(),
                    dateFilter = null
                )
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            placeholderId,
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        val manualActionUrl = "https://exponea.com"
        var stepIndex = 0
        var messageShown = 0
        var actionClicked = 0
        var noMessageFound = 0
        var shownMessage: InAppContentBlock? = null
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                assertEquals("id1", contentBlock.id)
                messageShown = ++stepIndex
                shownMessage = contentBlock
            }
            override fun onNoMessageFound(placeholderId: String) {
                noMessageFound = ++stepIndex
            }
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                fail("Should not throw error")
            }
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                fail("Should not invoke close click")
            }
            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                assertEquals("id1", contentBlock.id)
                assertEquals(manualActionUrl, action.url)
                actionClicked = ++stepIndex
            }
        }
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertEquals(1, messageShown)
        assertNotNull(shownMessage)
        placeholder.invokeActionClick(manualActionUrl)
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertEquals(2, actionClicked)
        assertEquals(3, noMessageFound)
        // message is visible 'until interaction' so next message should not be shown/found
        assertEquals(1, messageShown)
        // local flags validation
        val displayState = displayStateRepository.get(shownMessage!!)
        assertEquals(1, displayState.displayedCount)
        assertNotNull(displayState.displayedLast)
        assertEquals(1, displayState.interactedCount)
        assertNotNull(displayState.interactedLast)
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should store interaction flags by invoking close action`() {
        val placeholderId = "ph1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent()),
                    placeholders = listOf(placeholderId),
                    rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase(),
                    dateFilter = null
                )
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            placeholderId,
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        val manualCloseUrl = "https://exponea.com/close_action"
        var stepIndex = 0
        var messageShown = 0
        var actionClosed = 0
        var noMessageFound = 0
        var shownMessage: InAppContentBlock? = null
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                assertEquals("id1", contentBlock.id)
                messageShown = ++stepIndex
                shownMessage = contentBlock
            }
            override fun onNoMessageFound(placeholderId: String) {
                noMessageFound = ++stepIndex
            }
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                fail("Should not throw error")
            }
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                assertEquals("id1", contentBlock.id)
                actionClosed = ++stepIndex
            }
            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                fail("Should not invoke action click")
            }
        }
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertEquals(1, messageShown)
        assertNotNull(shownMessage)
        placeholder.invokeActionClick(manualCloseUrl)
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertEquals(2, actionClosed)
        assertEquals(3, noMessageFound)
        // message is visible 'until interaction' so next message should not be shown/found
        assertEquals(1, messageShown)
        // local flags validation
        val displayState = displayStateRepository.get(shownMessage!!)
        assertEquals(1, displayState.displayedCount)
        assertNotNull(displayState.displayedLast)
        assertEquals(1, displayState.interactedCount)
        assertNotNull(displayState.interactedLast)
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should store interaction flags by invoking invalid action`() {
        val placeholderId = "ph1"
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent()),
                    placeholders = listOf(placeholderId),
                    rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase(),
                    dateFilter = null
                )
            )))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            placeholderId,
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        val manualInvalidUrl = "https://exponea.com/is-not-listed-action"
        var stepIndex = 0
        var messageShown = 0
        var onErrorFound = 0
        var shownMessage: InAppContentBlock? = null
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                assertEquals("id1", contentBlock.id)
                messageShown = ++stepIndex
                shownMessage = contentBlock
            }
            override fun onNoMessageFound(placeholderId: String) {
                fail("Should not invoke no message step")
            }
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                onErrorFound = ++stepIndex
            }
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                fail("Should not invoke close click")
            }
            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                fail("Should not invoke action click")
            }
        }
        placeholder.refreshContent()
        shadowOf(getMainLooper()).idle()
        assertEquals(1, messageShown)
        assertNotNull(shownMessage)
        placeholder.invokeActionClick(manualInvalidUrl)
        shadowOf(getMainLooper()).idle()
        assertEquals(2, onErrorFound)
        // message is visible 'until interaction' so next message should not be shown/found
        assertEquals(1, messageShown)
        // local flags validation
        val displayState = displayStateRepository.get(shownMessage!!)
        assertEquals(1, displayState.displayedCount)
        assertNotNull(displayState.displayedLast)
        assertEquals(0, displayState.interactedCount)
        assertNull(displayState.interactedLast)
    }

    private fun preloadInAppContentBlocks(messages: ArrayList<InAppContentBlock>) {
        every { fetchManager.fetchStaticInAppContentBlocks(any(), any(), any()) } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, messages))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders()
    }

    private fun identifyCustomer(cookie: String? = null, ids: HashMap<String, String?> = hashMapOf()) {
        every { customerIdsRepository.get() } returns CustomerIds().apply {
            this.cookie = cookie
            this.externalIds = ids
        }
        inAppContentBlockManager.onEventCreated(Event(), EventType.TRACK_CUSTOMER)
    }
}

class InAppContentBlockDisplayStateMock : InAppContentBlockDisplayStateRepository {
    private val displayStates = mutableMapOf<String, InAppContentBlockDisplayState>()
    override fun get(message: InAppContentBlock): InAppContentBlockDisplayState {
        return displayStates[message.id] ?: InAppContentBlockDisplayState(
            null, 0, null, 0
        )
    }

    override fun setDisplayed(message: InAppContentBlock, date: Date) {
        val displayState = get(message)
        displayStates[message.id] = InAppContentBlockDisplayState(
            date,
            displayState.displayedCount + 1,
            displayState.interactedLast,
            displayState.interactedCount
        )
    }

    override fun setInteracted(message: InAppContentBlock, date: Date) {
        val displayState = get(message)
        displayStates[message.id] = InAppContentBlockDisplayState(
            displayState.displayedLast,
            displayState.displayedCount,
            date,
            displayState.interactedCount + 1
        )
    }

    override fun clear() {
        displayStates.clear()
    }
}
