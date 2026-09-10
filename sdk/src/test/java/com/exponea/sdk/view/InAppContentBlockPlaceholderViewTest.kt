package com.exponea.sdk.view

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.manager.FetchManager
import com.exponea.sdk.manager.InAppContentBlockManager
import com.exponea.sdk.manager.InAppContentBlockManagerImpl
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildHtmlMessageContent
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildMessage
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildMessageData
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.HtmlActionType
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockDisplayState
import com.exponea.sdk.models.InAppContentBlockFrequency
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.Result
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepository
import com.exponea.sdk.repository.VolatileInAppContentBlocksETagStore
import com.exponea.sdk.services.IntegrationConfigFactory
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockDataLoader
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.model.TelemetryEvent
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.RecordingLoggerCallback
import com.exponea.sdk.testutil.assertNonNegativeTimingFields
import com.exponea.sdk.testutil.latestInAppContentBlockTimingLog
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.parseInAppContentBlockTimingLog
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.buildCustomerIdsCacheKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockPlaceholderViewTest {

    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var drawableCache: DrawableCache
    private lateinit var fetchManager: FetchManager
    private lateinit var apiService: ExponeaService
    private lateinit var displayStateRepository: InAppContentBlockDisplayStateRepository
    private lateinit var projectFactory: IntegrationConfigFactory
    private lateinit var htmlCache: HtmlNormalizedCache
    private lateinit var fontCache: FontCache
    private lateinit var inAppContentBlockManager: InAppContentBlockManager

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fetchManager = mock()
        customerIdsRepository = mock()
        displayStateRepository = InAppContentBlockDisplayStateMock()
        drawableCache = mock {
            on { has(any()) } doReturn true
            doAnswer {
                it.getArgument<((Boolean) -> Unit)?>(1)?.invoke(true)
                null
            }.on { preload(any(), any()) }
            doNothing().on { clear() }
            on { getFile(any()) } doReturn MockFile()
        }
        fontCache = mock {
            on { has(any()) } doReturn true
            doAnswer {
                it.getArgument<((Boolean) -> Unit)?>(1)?.invoke(true)
                null
            }.on { preload(any(), any()) }
            on { getFontFile(any()) } doReturn MockFile()
        }
        val configuration = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                projectToken = "token",
                authorization = "Token auth",
                baseUrl = "https://test.com"
            )
        )
        projectFactory = IntegrationConfigFactory(configuration)
        htmlCache = mock {
            doNothing().on { remove(any()) }
            on { get(any(), any()) } doReturn null
            doNothing().on { set(any(), any(), any()) }
        }
        apiService = ExponeaMockService(true)
        inAppContentBlockManager = InAppContentBlockManagerImpl(
            displayStateRepository = displayStateRepository,
            fetchManager = fetchManager,
            integrationConfigFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
            imageCache = drawableCache,
            htmlCache = htmlCache,
            fontCache = fontCache,
            etagStore = VolatileInAppContentBlocksETagStore()
        )
        identifyCustomer()
    }

    @After
    fun after() {
        runCatching { unmockkObject(Exponea) }
        Exponea.safeModeOverride = null
        Exponea.telemetry = null
        Exponea.isStopped = false
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should load message assigned to placeholder ID`() = runInSingleThread { idleThreads ->
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders(emptyList())
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
        idleThreads()
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `load should refresh content without force refresh`() = runInSingleThread { idleThreads ->
        val placeholderId = "ph1"
        val contentBlocks = listOf(buildMessage("id1", placeholders = listOf(placeholderId)))
        val componentManager = mock<InAppContentBlockManager> {
            on { getAllInAppContentBlocksForPlaceholder(placeholderId) } doReturn contentBlocks
        }
        installExponeaComponent(componentManager)
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            placeholderId,
            noContentDataLoader(),
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
        )

        placeholder.load()
        idleThreads()

        verify(componentManager).getAllInAppContentBlocksForPlaceholder(placeholderId)
        verify(componentManager).loadContentIfNeededSync(contentBlocks, forceRefresh = false)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `reload should refresh content with force refresh`() = runInSingleThread { idleThreads ->
        val placeholderId = "ph1"
        val contentBlocks = listOf(buildMessage("id1", placeholders = listOf(placeholderId)))
        val componentManager = mock<InAppContentBlockManager> {
            on { getAllInAppContentBlocksForPlaceholder(placeholderId) } doReturn contentBlocks
        }
        installExponeaComponent(componentManager)
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            placeholderId,
            noContentDataLoader(),
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
        )

        placeholder.reload()
        idleThreads()

        verify(componentManager).getAllInAppContentBlocksForPlaceholder(placeholderId)
        verify(componentManager).loadContentIfNeededSync(contentBlocks, forceRefresh = true)
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should call message changed events in correct order`() = runInSingleThread { idleThreads ->
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            "placeholder_1",
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        preloadInAppContentBlocks(
            arrayListOf(
                buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
            )
        )
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
        idleThreads()
        assertTrue(messageFound)
        assertEquals(View.VISIBLE, placeholder.htmlContainer.visibility)
        assertEquals(View.GONE, placeholder.placeholder.visibility)
        preloadInAppContentBlocks(arrayListOf())
        placeholder.refreshContent()
        idleThreads()
        assertFalse(messageFound)
        assertEquals(View.GONE, placeholder.htmlContainer.visibility)
        assertEquals(View.VISIBLE, placeholder.placeholder.visibility)
        preloadInAppContentBlocks(
            arrayListOf(
                buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
            )
        )
        placeholder.refreshContent()
        idleThreads()
        assertTrue(messageFound)
        assertEquals(View.VISIBLE, placeholder.htmlContainer.visibility)
        assertEquals(View.GONE, placeholder.placeholder.visibility)
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should store interaction flags by invoking manual action`() = runInSingleThread { idleThreads ->
        val placeholderId = "ph1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1",
                            type = "html",
                            data = mapOf("html" to buildHtmlMessageContent()),
                            placeholders = listOf(placeholderId),
                            rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase(),
                            dateFilter = null
                        )
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders(emptyList())
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
        idleThreads()
        assertEquals(1, messageShown)
        assertNotNull(shownMessage)
        placeholder.invokeActionClick(manualActionUrl)
        placeholder.refreshContent()
        idleThreads()
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
    fun `should invoke callbacks safely`() = runInSingleThread { idleThreads ->
        Exponea.safeModeEnabled = true
        val placeholderId = "ph1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1",
                            type = "html",
                            data = mapOf("html" to buildHtmlMessageContent()),
                            placeholders = listOf(placeholderId),
                            rawFrequency = InAppContentBlockFrequency.ALWAYS.name.lowercase(),
                            dateFilter = null
                        )
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders(emptyList())
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            placeholderId,
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        val manualActionUrl = "https://exponea.com"
        var onMessageShownCalled = false
        var onNoMessageFoundCalled = false
        var onErrorCalled = false
        var onCloseClickedCalled = false
        var onActionClickedCalled = false
        var onHeightUpdateCalled = false
        var onContentReadyCalled = false
        placeholder.setOnContentReadyListener {
            onContentReadyCalled = true
            throw RuntimeException("Test error")
        }
        placeholder.setOnHeightUpdateListener {
            onHeightUpdateCalled = true
            throw RuntimeException("Test error")
        }
        placeholder.behaviourCallback = object : InAppContentBlockCallback {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                onMessageShownCalled = true
                throw RuntimeException("Test error")
            }

            override fun onNoMessageFound(placeholderId: String) {
                onNoMessageFoundCalled = true
                throw RuntimeException("Test error")
            }

            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                onErrorCalled = true
                throw RuntimeException("Test error")
            }

            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                onCloseClickedCalled = true
                throw RuntimeException("Test error")
            }

            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                onActionClickedCalled = true
                throw RuntimeException("Test error")
            }
        }
        // Simulate WebView on page loaded event, then layout change
        placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
        placeholder.layout(0, 0, 10, 10)
        idleThreads()
        // simulates action click
        placeholder.refreshContent()
        idleThreads()
        placeholder.invokeActionClick(manualActionUrl)
        // simulates closing
        placeholder.refreshContent()
        idleThreads()
        val manualCloseUrl = placeholder.controller.assignedHtmlContent?.actions?.find {
            it.actionType == HtmlActionType.CLOSE
        }
        placeholder.invokeActionClick(manualCloseUrl?.actionUrl!!)
        // simulates onError - action not found
        placeholder.refreshContent()
        idleThreads()
        placeholder.invokeActionClick("non-existing")
        // simulates no message found
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, arrayListOf()))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders(emptyList())
        placeholder.refreshContent()
        idleThreads()
        assertTrue(onMessageShownCalled)
        assertTrue(onNoMessageFoundCalled)
        assertTrue(onErrorCalled)
        assertTrue(onCloseClickedCalled)
        assertTrue(onActionClickedCalled)
        assertTrue(onHeightUpdateCalled)
        assertTrue(onContentReadyCalled)
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should store interaction flags by invoking close action`() = runInSingleThread { idleThreads ->
        val placeholderId = "ph1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1",
                            type = "html",
                            data = mapOf("html" to buildHtmlMessageContent()),
                            placeholders = listOf(placeholderId),
                            rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase(),
                            dateFilter = null
                        )
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders(emptyList())
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            placeholderId,
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
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
        idleThreads()
        assertEquals(1, messageShown)
        assertNotNull(shownMessage)
        val manualCloseUrl = placeholder.controller.assignedHtmlContent?.actions?.find {
            it.actionType == HtmlActionType.CLOSE
        }
        placeholder.invokeActionClick(manualCloseUrl?.actionUrl!!)
        placeholder.refreshContent()
        idleThreads()
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
    fun `should store interaction flags by invoking invalid action`() = runInSingleThread { idleThreads ->
        val placeholderId = "ph1"
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(
                Result(
                    true, arrayListOf(
                        buildMessage(
                            "id1",
                            type = "html",
                            data = mapOf("html" to buildHtmlMessageContent()),
                            placeholders = listOf(placeholderId),
                            rawFrequency = InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS.name.lowercase(),
                            dateFilter = null
                        )
                    )
                )
            )
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders(emptyList())
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
        idleThreads()
        assertEquals(1, messageShown)
        assertNotNull(shownMessage)
        placeholder.invokeActionClick(manualInvalidUrl)
        idleThreads()
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

    @Test
    fun `should call content ready - pageLoaded then layouted`() {
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            "placeholder_1",
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        preloadInAppContentBlocks(
            arrayListOf(
                buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
            )
        )
        val messageShownInvoked = java.util.concurrent.Semaphore(0, true)
        placeholder.behaviourCallback = object : EmptyInAppContentBlockCallback() {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                messageShownInvoked.release()
            }
        }
        val contentReadyInvoked = CountDownLatch(1)
        placeholder.setOnContentReadyListener {
            contentReadyInvoked.countDown()
        }
        placeholder.refreshContent()
        assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
        // Simulate WebView on page loaded event, then layout change
        placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
        placeholder.layout(0, 0, 10, 10)
        assertTrue(contentReadyInvoked.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun `should call content ready - pageLoaded without layouted`() {
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            "placeholder_1",
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        preloadInAppContentBlocks(
            arrayListOf(
                buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
            )
        )
        val messageShownInvoked = java.util.concurrent.Semaphore(0, true)
        placeholder.behaviourCallback = object : EmptyInAppContentBlockCallback() {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                messageShownInvoked.release()
            }
        }
        val contentReadyInvoked = CountDownLatch(1)
        placeholder.setOnContentReadyListener {
            contentReadyInvoked.countDown()
        }
        placeholder.refreshContent()
        assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
        // Simulate WebView on page loaded event, then layout change
        placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
        assertTrue(contentReadyInvoked.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun `should notify height update only after content is ready`() {
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            "placeholder_1",
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        var heightUpdateCount = 0
        placeholder.setOnHeightUpdateListener {
            heightUpdateCount++
        }

        placeholder.layout(0, 0, 10, 10)
        assertEquals(0, heightUpdateCount)

        placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
        placeholder.layout(0, 0, 10, 20)
        assertEquals(1, heightUpdateCount)
    }

    @Test
    fun `should track render completion when content ready`() = runInSingleThread { idleThreads ->
        val telemetryEventCaptor = argumentCaptor<TelemetryEvent>()
        val telemetryPropertiesCaptor = argumentCaptor<MutableMap<String, String>>()
        Exponea.telemetry = mock<TelemetryManager> {
            doNothing().on {
                reportEvent(
                    telemetryEventCaptor.capture(),
                    telemetryPropertiesCaptor.capture()
                )
            }
        }
        try {
            val placeholder = inAppContentBlockManager.getPlaceholderView(
                "placeholder_1",
                ApplicationProvider.getApplicationContext(),
                InAppContentBlockPlaceholderConfiguration(true)
            )
            preloadInAppContentBlocks(
                arrayListOf(
                    buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
                )
            )
            val messageShownInvoked = java.util.concurrent.Semaphore(0, true)
            placeholder.behaviourCallback = object : EmptyInAppContentBlockCallback() {
                override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                    messageShownInvoked.release()
                }
            }
            val contentReadyInvoked = CountDownLatch(1)
            placeholder.setOnContentReadyListener {
                contentReadyInvoked.countDown()
            }

            placeholder.refreshContent()
            idleThreads()
            assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
            idleThreads()
            placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
            placeholder.layout(0, 0, 10, 10)
            assertTrue(contentReadyInvoked.await(2, TimeUnit.SECONDS))

            val renderedEventIndex = telemetryEventCaptor.allValues.indexOf(TelemetryEvent.CONTENT_BLOCK_RENDERED)
            assertTrue(renderedEventIndex >= 0, "Render telemetry was not tracked")
            val capturedProps = telemetryPropertiesCaptor.allValues[renderedEventIndex]
            assertEquals("placeholder_1", capturedProps["placeholderId"])
            assertEquals("id1", capturedProps["messageId"])
            assertEquals("true", capturedProps["contentLoaded"])
            assertEquals("webview_visual_state", capturedProps["finishSource"])
            assertEquals("rendered", capturedProps["result"])
            assertNull(capturedProps["durationMs"])
            assertNull(capturedProps["requestToRenderMs"])
            assertNull(capturedProps["showToRenderMs"])
        } finally {
            Exponea.telemetry = null
        }
    }

    @Test
    fun `should emit timing debug log when html content is ready`() = runInSingleThread { idleThreads ->
        val loggerCallback = RecordingLoggerCallback()
        val previousLoggerLevel = Logger.level
        Logger.level = Logger.Level.DEBUG
        Exponea.registerLoggerCallback(loggerCallback)
        try {
            val placeholder = inAppContentBlockManager.getPlaceholderView(
                "placeholder_1",
                ApplicationProvider.getApplicationContext(),
                InAppContentBlockPlaceholderConfiguration(true)
            )
            preloadInAppContentBlocks(
                arrayListOf(
                    buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
                )
            )
            val messageShownInvoked = java.util.concurrent.Semaphore(0, true)
            placeholder.behaviourCallback = object : EmptyInAppContentBlockCallback() {
                override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                    messageShownInvoked.release()
                }
            }

            placeholder.refreshContent()
            idleThreads()
            assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
            placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
            placeholder.layout(0, 0, 10, 10)
            idleThreads()

            val (level, message) = latestInAppContentBlockTimingLog(loggerCallback)
            assertEquals(Logger.Level.DEBUG, level)
            val fields = parseInAppContentBlockTimingLog(message)
            assertEquals("id1", fields["contentBlockId"])
            assertEquals("render_finished", fields["source"])
            assertEquals("true", fields["contentLoaded"])
            assertEquals("webview_visual_state", fields["finishSource"])
            assertNonNegativeTimingFields(
                fields,
                "timestampMs"
            )
        } finally {
            Exponea.unregisterLoggerCallback(loggerCallback)
            Logger.level = previousLoggerLevel
        }
    }

    @Test
    fun `should emit timing debug log when no content is ready`() = runInSingleThread { idleThreads ->
        val loggerCallback = RecordingLoggerCallback()
        Exponea.registerLoggerCallback(loggerCallback)
        try {
            val placeholder = inAppContentBlockManager.getPlaceholderView(
                "ph1",
                noContentDataLoader(),
                ApplicationProvider.getApplicationContext(),
                InAppContentBlockPlaceholderConfiguration(defferedLoad = true)
            )

            placeholder.refreshContent()
            idleThreads()

            assertTrue(loggerCallback.logs.none { it.second.startsWith("InAppCB timing: ") })
        } finally {
            Exponea.unregisterLoggerCallback(loggerCallback)
        }
    }

    @Test
    fun `should emit timing debug log with stored etag mode`() = runInSingleThread { idleThreads ->
        val loggerCallback = RecordingLoggerCallback()
        val previousLoggerLevel = Logger.level
        Logger.level = Logger.Level.DEBUG
        Exponea.registerLoggerCallback(loggerCallback)
        try {
            val etagStore = VolatileInAppContentBlocksETagStore()
            val manager = replaceManager(etagStore)
            val placeholderId = "ph1"
            val block = buildMessage("id1", placeholders = listOf(placeholderId)).apply {
                customerIds = customerIdsRepository.get().toHashMap()
            }
            manager.contentBlocksData = listOf(block)
            etagStore.store(etagCacheKey(block.customerIds, listOf(block.id)), "\"etag-v1\"")
            whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), anyOrNull(), anyOrNull(),
            anyOrNull(), any(), any())).thenAnswer {
                it.getArgument<((String) -> Unit)?>(5)?.invoke("\"etag-v2\"")
                it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(
                    6
                ).invoke(
                    Result(
                        true,
                        arrayListOf(
                            buildMessageData(
                                block.id,
                                ttl = 60,
                                type = "html",
                                data = mapOf("html" to buildHtmlMessageContent())
                            )
                        )
                    )
                )
                null
            }

            val fields = renderAndReadTimingFields(
                manager, placeholderId, loggerCallback, idleThreads, "etag_looked_up", "stored"
            )

            assertEquals("stored", fields["etagMode"])
            assertNonNegativeTimingFields(fields, "timestampMs")
        } finally {
            Exponea.unregisterLoggerCallback(loggerCallback)
            Logger.level = previousLoggerLevel
        }
    }

    @Test
    fun `should emit timing debug log with not modified etag mode`() = runInSingleThread { idleThreads ->
        val loggerCallback = RecordingLoggerCallback()
        val previousLoggerLevel = Logger.level
        Logger.level = Logger.Level.DEBUG
        Exponea.registerLoggerCallback(loggerCallback)
        try {
            val etagStore = VolatileInAppContentBlocksETagStore()
            val manager = replaceManager(etagStore)
            val placeholderId = "ph1"
            val block = buildMessage("id1", placeholders = listOf(placeholderId)).apply {
                customerIds = customerIdsRepository.get().toHashMap()
                personalizedData = buildMessageData(
                    id,
                    ttl = 1,
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent())
                ).apply { loadedAt = Date(System.currentTimeMillis() - 60_000) }
            }
            manager.contentBlocksData = listOf(block)
            etagStore.store(etagCacheKey(block.customerIds, listOf(block.id)), "\"etag-v1\"")
            whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), anyOrNull(), anyOrNull(),
            anyOrNull(), any(), any())).thenAnswer {
                it.getArgument<(() -> Unit)?>(4)?.invoke()
                null
            }

            val fields = renderAndReadTimingFields(
                manager, placeholderId, loggerCallback, idleThreads, "network_fetch_finished", "not_modified"
            )

            assertEquals("not_modified", fields["etagMode"])
            assertNonNegativeTimingFields(fields, "timestampMs")
        } finally {
            Exponea.unregisterLoggerCallback(loggerCallback)
            Logger.level = previousLoggerLevel
        }
    }

    @Test
    fun `should emit timing debug log with retry without etag mode`() = runInSingleThread { idleThreads ->
        val loggerCallback = RecordingLoggerCallback()
        val previousLoggerLevel = Logger.level
        Logger.level = Logger.Level.DEBUG
        Exponea.registerLoggerCallback(loggerCallback)
        try {
            val etagStore = VolatileInAppContentBlocksETagStore()
            val manager = replaceManager(etagStore)
            val placeholderId = "ph1"
            val block = buildMessage("id1", placeholders = listOf(placeholderId)).apply {
                customerIds = customerIdsRepository.get().toHashMap()
            }
            manager.contentBlocksData = listOf(block)
            etagStore.store(etagCacheKey(block.customerIds, listOf(block.id)), "\"etag-v1\"")
            var fetchCount = 0
            whenever(fetchManager.fetchPersonalizedContentBlocks(any(), any(), any(), anyOrNull(), anyOrNull(),
            anyOrNull(), any(), any())).thenAnswer {
                fetchCount++
                if (fetchCount == 1) {
                    it.getArgument<(() -> Unit)?>(4)?.invoke()
                } else {
                    it.getArgument<(Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit>(
                        6
                    ).invoke(
                        Result(
                            true,
                            arrayListOf(
                                buildMessageData(
                                    block.id,
                                    ttl = 60,
                                    type = "html",
                                    data = mapOf("html" to buildHtmlMessageContent())
                                )
                            )
                        )
                    )
                }
                null
            }

            val fields = renderAndReadTimingFields(
                manager, placeholderId, loggerCallback, idleThreads, "network_fetch_finished", "retry_without_etag"
            )

            assertEquals("retry_without_etag", fields["etagMode"])
            assertNonNegativeTimingFields(fields, "timestampMs")
        } finally {
            Exponea.unregisterLoggerCallback(loggerCallback)
            Logger.level = previousLoggerLevel
        }
    }

    @Test
    fun `should track render completion with timeout finish source`() = runInSingleThread { idleThreads ->
        val telemetryEventCaptor = argumentCaptor<TelemetryEvent>()
        val telemetryPropertiesCaptor = argumentCaptor<MutableMap<String, String>>()
        Exponea.telemetry = mock<TelemetryManager> {
            doNothing().on {
                reportEvent(
                    telemetryEventCaptor.capture(),
                    telemetryPropertiesCaptor.capture()
                )
            }
        }
        try {
            val placeholder = inAppContentBlockManager.getPlaceholderView(
                "placeholder_1",
                ApplicationProvider.getApplicationContext(),
                InAppContentBlockPlaceholderConfiguration(true)
            )
            preloadInAppContentBlocks(
                arrayListOf(
                    buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
                )
            )
            val messageShownInvoked = java.util.concurrent.Semaphore(0, true)
            placeholder.behaviourCallback = object : EmptyInAppContentBlockCallback() {
                override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                    messageShownInvoked.release()
                }
            }
            val contentReadyInvoked = CountDownLatch(1)
            placeholder.setOnContentReadyListener {
                contentReadyInvoked.countDown()
            }

            placeholder.refreshContent()
            idleThreads()
            assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
            idleThreads()
            placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_timeout")
            assertTrue(contentReadyInvoked.await(2, TimeUnit.SECONDS))

            val renderedEventIndex = telemetryEventCaptor.allValues.indexOf(TelemetryEvent.CONTENT_BLOCK_RENDERED)
            assertTrue(renderedEventIndex >= 0, "Render telemetry was not tracked")
            val capturedProps = telemetryPropertiesCaptor.allValues[renderedEventIndex]
            assertEquals("placeholder_1", capturedProps["placeholderId"])
            assertEquals("id1", capturedProps["messageId"])
            assertEquals("true", capturedProps["contentLoaded"])
            assertEquals("webview_timeout", capturedProps["finishSource"])
            assertEquals("rendered", capturedProps["result"])
            assertNull(capturedProps["durationMs"])
            assertNull(capturedProps["requestToRenderMs"])
            assertNull(capturedProps["showToRenderMs"])
        } finally {
            Exponea.telemetry = null
        }
    }

    @Test
    fun `should call content ready - pageLoaded multiple times with first layout`() {
        val placeholder = inAppContentBlockManager.getPlaceholderView(
            "placeholder_1",
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        preloadInAppContentBlocks(
            arrayListOf(
                buildMessage("id1", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
            )
        )
        val messageShownInvoked = java.util.concurrent.Semaphore(0, true)
        placeholder.behaviourCallback = object : EmptyInAppContentBlockCallback() {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                messageShownInvoked.release()
            }
        }
        val contentReadyInvoked = java.util.concurrent.Semaphore(0, true)
        placeholder.setOnContentReadyListener {
            contentReadyInvoked.release()
        }
        // first load, onPageLoadedCallback called, layoutChange called
        placeholder.refreshContent()
        assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
        // Simulate WebView on page loaded event, then layout change
        placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
        placeholder.layout(0, 0, 10, 10)
        assertTrue(contentReadyInvoked.tryAcquire(2, TimeUnit.SECONDS))
        // second load, onPageLoadedCallback called, layoutChange not called
        placeholder.refreshContent()
        assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
        // Simulate WebView on page loaded event, then layout change
        placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
        assertTrue(contentReadyInvoked.tryAcquire(2, TimeUnit.SECONDS))
    }

    private fun replaceManager(etagStore: VolatileInAppContentBlocksETagStore): InAppContentBlockManagerImpl {
        inAppContentBlockManager = InAppContentBlockManagerImpl(
            displayStateRepository = displayStateRepository,
            fetchManager = fetchManager,
            integrationConfigFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
            imageCache = drawableCache,
            htmlCache = htmlCache,
            fontCache = fontCache,
            etagStore = etagStore
        )
        identifyCustomer(cookie = "cookie-1", ids = hashMapOf("login" to "test"))
        return inAppContentBlockManager as InAppContentBlockManagerImpl
    }

    private fun etagCacheKey(customerIds: Map<String, String?>, contentBlockIds: List<String>): String {
        val sortedBlockIds = contentBlockIds.distinct().sorted().joinToString(",")
        return "${buildCustomerIdsCacheKey(customerIds)}|$sortedBlockIds"
    }

    private fun renderAndReadTimingFields(
        manager: InAppContentBlockManagerImpl,
        placeholderId: String,
        loggerCallback: RecordingLoggerCallback,
        idleThreads: () -> Unit,
        source: String,
        etagMode: String
    ): Map<String, String> {
        val placeholder = manager.getPlaceholderView(
            placeholderId,
            ApplicationProvider.getApplicationContext(),
            InAppContentBlockPlaceholderConfiguration(true)
        )
        val messageShownInvoked = java.util.concurrent.Semaphore(0, true)
        placeholder.behaviourCallback = object : EmptyInAppContentBlockCallback() {
            override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {
                messageShownInvoked.release()
            }
        }

        placeholder.refreshContent()
        idleThreads()
        assertTrue(messageShownInvoked.tryAcquire(2, TimeUnit.SECONDS), "Message was not loaded yet")
        placeholder.htmlContainer.onPageLoadedCallback?.invoke("webview_visual_state")
        idleThreads()

        val (level, message) = latestInAppContentBlockTimingLog(loggerCallback, source, etagMode)
        assertEquals(Logger.Level.DEBUG, level)
        return parseInAppContentBlockTimingLog(message)
    }

    private fun preloadInAppContentBlocks(messages: ArrayList<InAppContentBlock>) {
        whenever(fetchManager.fetchStaticInAppContentBlocks(any(), any(), any())).thenAnswer {
            it.getArgument<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, messages))
        }
        inAppContentBlockManager.loadInAppContentBlockPlaceholders(emptyList())
    }

    private fun identifyCustomer(cookie: String? = null, ids: HashMap<String, String?> = hashMapOf()) {
        whenever(customerIdsRepository.get()).thenReturn(
            CustomerIds().apply {
                this.cookie = cookie
                this.externalIds = ids
            }
        )
        inAppContentBlockManager.onEventCreated(Event(), EventType.TRACK_CUSTOMER)
    }

    private fun noContentDataLoader(): InAppContentBlockDataLoader {
        return object : InAppContentBlockDataLoader {
            override fun loadContent(placeholderId: String): InAppContentBlock? {
                return null
            }
        }
    }

    private fun installExponeaComponent(manager: InAppContentBlockManager) {
        val component = mockk<ExponeaComponent>()
        val configuration = ExponeaConfiguration(
            integrationConfig = ProjectConfig(
                projectToken = "token",
                authorization = "Token auth",
                baseUrl = "https://test.com"
            )
        )
        mockkObject(Exponea)
        every { Exponea.isStopped } returns false
        every { Exponea.getComponent() } returns component
        every { component.exponeaConfiguration } returns configuration
        every { component.inAppContentBlockManager } returns manager
    }
}

open class EmptyInAppContentBlockCallback : InAppContentBlockCallback {
    override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock) {}
    override fun onNoMessageFound(placeholderId: String) {}
    override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {}
    override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {}
    override fun onActionClicked(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: InAppContentBlockAction
    ) {
    }
}

class InAppContentBlockDisplayStateMock : InAppContentBlockDisplayStateRepository {
    private val displayStates = mutableMapOf<String, InAppContentBlockDisplayState>()
    override fun get(message: InAppContentBlock): InAppContentBlockDisplayState {
        return displayStates[message.id] ?: InAppContentBlockDisplayState(
            null, 0, null, 0
        )
    }

    override fun getAll(): Map<String, InAppContentBlockDisplayState> {
        return displayStates.toMap()
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
