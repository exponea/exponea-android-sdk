package com.exponea.sdk.view

import android.content.Context
import androidx.browser.customtabs.CustomTabsClient
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.InAppContentBlockManagerImpl
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildHtmlMessageContent
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildMessage
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.ContentBlockCarouselCallback
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.HtmlActionType
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselAdapter
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.model.EventType
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.reset
import com.exponea.sdk.testutil.runInSingleThread
import com.exponea.sdk.testutil.shutdown
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockCarouselViewTest : ExponeaSDKTest() {

    @Before
    fun before() {
        // Cold start
        Exponea.reset()
        Exponea.shutdown()
        // CustomTabsClient service off
        mockkStatic(CustomTabsClient::class)
        every {
            CustomTabsClient.bindCustomTabsService(any(), any(), any())
        } returns true
        mockkConstructorFix(FetchManagerImpl::class) {
            every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) }
        }
        mockkConstructorFix(InAppContentBlockManagerImpl::class)
        // mock image and font resources
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
        mockkConstructorFix(FontCacheImpl::class) {
            every { anyConstructed<FontCacheImpl>().has(any()) }
        }
        every { anyConstructed<FontCacheImpl>().getFontFile(any()) } returns File(
            this.javaClass.classLoader!!.getResource("xtrusion.ttf")!!.file
        )
        every { anyConstructed<FontCacheImpl>().has(any()) } returns true
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should load message assigned to placeholder ID`() = runInSingleThread { idleThreads ->
        prepareContentBlockMessages(
            arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent())
                )
            ))
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        mockkConstructorFix(TelemetryManager::class)
        val telemetryEventTypeSlot = slot<EventType>()
        val telemetryPropertiesSlot = slot<MutableMap<String, String>>()
        every {
            anyConstructed<TelemetryManager>().reportEvent(
                capture(telemetryEventTypeSlot),
                capture(telemetryPropertiesSlot)
            )
        } just Runs
        Exponea.telemetry = TelemetryManager(ApplicationProvider.getApplicationContext())
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        carousel.reload()
        idleThreads()
        assertTrue(telemetryEventTypeSlot.isCaptured)
        val capturedEventType = telemetryEventTypeSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(EventType.SHOW_IN_APP_MESSAGE, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertEquals("content_block_carousel", capturedProps["messageType"])
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should notify shown message assigned to placeholder ID`() = runInSingleThread { idleThreads ->
        prepareContentBlockMessages(
            arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent())
                )
            ))
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        var shownPlaceholderId: String? = null
        var shownContentBlock: InAppContentBlock? = null
        carousel.behaviourCallback = object : EmptyCarouselBehaviourCallback() {
            override fun onMessageShown(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                index: Int,
                count: Int
            ) {
                shownPlaceholderId = placeholderId
                shownContentBlock = contentBlock
            }
        }
        carousel.reload()
        idleThreads()
        assertEquals("placeholder_1", shownPlaceholderId)
        assertNotNull(shownContentBlock)
        assertEquals("id1", shownContentBlock?.id)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should notify not-shown message`() = runInSingleThread { idleThreads ->
        prepareContentBlockMessages(
            arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent())
                )
            ))
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_2"
        )
        assertNotNull(carousel)
        var notifiedPlaceholderId: String? = null
        carousel.behaviourCallback = object : EmptyCarouselBehaviourCallback() {
            override fun onNoMessageFound(placeholderId: String) {
                notifiedPlaceholderId = placeholderId
            }
        }
        carousel.reload()
        idleThreads()
        assertEquals("placeholder_2", notifiedPlaceholderId)
    }

    @Test
    fun `should sort messages by name as for same priority`() = runInSingleThread { idleThreads ->
        val htmlMessageContent = mapOf("html" to buildHtmlMessageContent())
        val messages = arrayListOf(
            buildMessage("id1", priority = 10, name = "D", data = htmlMessageContent, type = "html"),
            buildMessage("id2", priority = 10, name = "B", data = htmlMessageContent, type = "html"),
            buildMessage("id3", priority = 10, name = "C", data = htmlMessageContent, type = "html"),
            buildMessage("id4", priority = 10, name = "A", data = htmlMessageContent, type = "html")
        )
        prepareContentBlockMessages(messages)
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        carousel.reload()
        idleThreads()
        val loadedMessages = carousel.viewController.contentBlockCarouselAdapter.getLoadedData()
        assertEquals(4, loadedMessages.size)
        assertEquals("id4", loadedMessages[0].id)
        assertEquals("id2", loadedMessages[1].id)
        assertEquals("id3", loadedMessages[2].id)
        assertEquals("id1", loadedMessages[3].id)
    }

    @Test
    fun `should sort messages by priority primary`() = runInSingleThread { idleThreads ->
        val htmlMessageContent = mapOf("html" to buildHtmlMessageContent())
        val messages = arrayListOf(
            buildMessage("id1", priority = 10, name = "D", data = htmlMessageContent, type = "html"),
            buildMessage("id2", priority = 20, name = "C", data = htmlMessageContent, type = "html"),
            buildMessage("id3", priority = 30, name = "B", data = htmlMessageContent, type = "html"),
            buildMessage("id4", priority = 40, name = "A", data = htmlMessageContent, type = "html")
        )
        prepareContentBlockMessages(messages)
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        carousel.reload()
        idleThreads()
        val loadedMessages = carousel.viewController.contentBlockCarouselAdapter.getLoadedData()
        assertEquals(4, loadedMessages.size)
        assertEquals("id4", loadedMessages[0].id)
        assertEquals("id3", loadedMessages[1].id)
        assertEquals("id2", loadedMessages[2].id)
        assertEquals("id1", loadedMessages[3].id)
    }

    @Test
    fun `should sort messages by custom impl`() = runInSingleThread { idleThreads ->
        val htmlMessageContent = mapOf("html" to buildHtmlMessageContent())
        val messages = arrayListOf(
            buildMessage("id1", priority = 10, name = "D", data = htmlMessageContent, type = "html"),
            buildMessage("id2", priority = 20, name = "C", data = htmlMessageContent, type = "html"),
            buildMessage("id3", priority = 30, name = "B", data = htmlMessageContent, type = "html"),
            buildMessage("id4", priority = 40, name = "A", data = htmlMessageContent, type = "html")
        )
        prepareContentBlockMessages(messages)
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        carousel?.contentBlockSelector = object : ContentBlockSelector() {
            override fun sortContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                // default result is id4, id3, id2, id1
                val defaultSort = super.sortContentBlocks(source)
                // shuffle and miss index 2 by purpose
                return arrayListOf(defaultSort[1], defaultSort[3], defaultSort[0])
            }
        }
        assertNotNull(carousel)
        carousel.reload()
        idleThreads()
        val loadedMessages = carousel.viewController.contentBlockCarouselAdapter.getLoadedData()
        assertEquals(3, loadedMessages.size)
        assertEquals("id3", loadedMessages[0].id)
        assertEquals("id1", loadedMessages[1].id)
        assertEquals("id4", loadedMessages[2].id)
    }

    @Test
    fun `should filter messages by custom impl`() = runInSingleThread { idleThreads ->
        val htmlMessageContent = mapOf("html" to buildHtmlMessageContent())
        val messages = arrayListOf(
            buildMessage("id1", priority = 10, name = "D", data = htmlMessageContent, type = "html"),
            buildMessage("id2", priority = 20, name = "C", data = htmlMessageContent, type = "html"),
            buildMessage("id3", priority = 30, name = "B", data = htmlMessageContent, type = "html"),
            buildMessage("id4", priority = 40, name = "A", data = htmlMessageContent, type = "html")
        )
        prepareContentBlockMessages(messages)
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        carousel?.contentBlockSelector = object : ContentBlockSelector() {
            override fun filterContentBlocks(source: List<InAppContentBlock>): List<InAppContentBlock> {
                return source.filter { it.id == "id3" }
            }
        }
        assertNotNull(carousel)
        carousel.reload()
        idleThreads()
        val loadedMessages = carousel.viewController.contentBlockCarouselAdapter.getLoadedData()
        assertEquals(1, loadedMessages.size)
        assertEquals("id3", loadedMessages[0].id)
    }

    @Test
    fun `should shown valid counter info after reload`() = runInSingleThread { idleThreads ->
        val htmlMessageContent = mapOf("html" to buildHtmlMessageContent())
        val messages = arrayListOf(
            buildMessage("id1", priority = 10, name = "D", data = htmlMessageContent, type = "html"),
            buildMessage("id2", priority = 20, name = "C", data = htmlMessageContent, type = "html"),
            buildMessage("id3", priority = 30, name = "B", data = htmlMessageContent, type = "html"),
            buildMessage("id4", priority = 40, name = "A", data = htmlMessageContent, type = "html")
        )
        prepareContentBlockMessages(arrayListOf())
        initSdk()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        carousel.reload()
        idleThreads()
        assertNull(carousel.getShownContentBlock())
        assertEquals(-1, carousel.getShownIndex())
        assertEquals(0, carousel.getShownCount())
        prepareContentBlockMessages(messages)
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        carousel.reload()
        idleThreads()
        assertNotNull(carousel.getShownContentBlock())
        assertEquals(0, carousel.getShownIndex())
        assertEquals(4, carousel.getShownCount())
    }

    @Test
    fun `should track show only once per message`() = runInSingleThread { idleThreads ->
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        val htmlMessageContent = mapOf("html" to buildHtmlMessageContent())
        val messages = arrayListOf(
            buildMessage("id1", priority = 30, name = "D", data = htmlMessageContent, type = "html"),
            buildMessage("id2", priority = 20, name = "C", data = htmlMessageContent, type = "html"),
            buildMessage("id3", priority = 10, name = "B", data = htmlMessageContent, type = "html")
        )
        prepareContentBlockMessages(messages)
        initSdk()
        idleThreads()
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<com.exponea.sdk.models.EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), any())
        } just Runs
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        idleThreads()
        carousel.reload()
        idleThreads()
        for (i in 0..100) {
            carousel.viewController.moveToIndex(0, false)
            idleThreads()
            carousel.viewController.moveToIndex(1, false)
            idleThreads()
            carousel.viewController.moveToIndex(2, false)
            idleThreads()
        }
        verify(exactly = 3) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any())
        }
        assertEquals("banner", eventSlot.captured.type)
        assertEquals(com.exponea.sdk.models.EventType.BANNER, eventTypeSlot.captured)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should notify error for message`() = runInSingleThread { idleThreads ->
        prepareContentBlockMessages(
            arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent())
                )
            ))
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        var notifiedPlaceholderId: String? = null
        var notifiedContentBlock: InAppContentBlock? = null
        var notifiedError: String? = null
        carousel.behaviourCallback = object : EmptyCarouselBehaviourCallback() {
            override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
                notifiedPlaceholderId = placeholderId
                notifiedContentBlock = contentBlock
                notifiedError = errorMessage
            }
        }
        var createdCbView: InAppContentBlockPlaceholderView? = null
        carousel.viewController.contentBlockCarouselAdapter = ContentBlockCarouselAdapter(
            placeholderId = "placeholder_1",
            onPlaceholderCreated = {
                carousel.viewController.modifyPlaceholderBehaviour(it)
                createdCbView = it
            }
        )
        carousel.reload()
        idleThreads()
        val cbViewHolder = carousel.viewController.contentBlockCarouselAdapter.createViewHolder(carousel, 0)
        cbViewHolder.updateContent(carousel.getShownContentBlock())
        assertNotNull(createdCbView)
        createdCbView?.controller?.loadContent(false)
        createdCbView?.controller?.onUrlClick("invalid_url")
        assertEquals("placeholder_1", notifiedPlaceholderId)
        assertNotNull(notifiedContentBlock)
        assertEquals("id1", notifiedContentBlock?.id)
        assertEquals("Invalid action definition", notifiedError)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should notify click for message`() = runInSingleThread { idleThreads ->
        prepareContentBlockMessages(
            arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent())
                )
            ))
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        var notifiedPlaceholderId: String? = null
        var notifiedContentBlock: InAppContentBlock? = null
        var notifiedAction: InAppContentBlockAction? = null
        carousel.behaviourCallback = object : EmptyCarouselBehaviourCallback() {
            override val overrideDefaultBehavior = true
            override fun onActionClicked(
                placeholderId: String,
                contentBlock: InAppContentBlock,
                action: InAppContentBlockAction
            ) {
                notifiedPlaceholderId = placeholderId
                notifiedContentBlock = contentBlock
                notifiedAction = action
            }
        }
        var createdCbView: InAppContentBlockPlaceholderView? = null
        carousel.viewController.contentBlockCarouselAdapter = ContentBlockCarouselAdapter(
            placeholderId = "placeholder_1",
            onPlaceholderCreated = {
                carousel.viewController.modifyPlaceholderBehaviour(it)
                createdCbView = it
            }
        )
        carousel.reload()
        idleThreads()
        val cbViewHolder = carousel.viewController.contentBlockCarouselAdapter.createViewHolder(carousel, 0)
        cbViewHolder.updateContent(carousel.getShownContentBlock())
        assertNotNull(createdCbView)
        createdCbView?.controller?.loadContent(false)
        createdCbView?.controller?.onUrlClick("https://exponea.com?xnpe_force_track=true")
        assertEquals("placeholder_1", notifiedPlaceholderId)
        assertNotNull(notifiedContentBlock)
        assertEquals("id1", notifiedContentBlock?.id)
        assertEquals("https://exponea.com?xnpe_force_track=true", notifiedAction?.url)
    }

    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should notify close for message`() = runInSingleThread { idleThreads ->
        prepareContentBlockMessages(
            arrayListOf(
                buildMessage(
                    "id1",
                    type = "html",
                    data = mapOf("html" to buildHtmlMessageContent())
                )
            ))
        initSdk()
        idleThreads()
        Exponea.componentForTesting.inAppContentBlockManager.loadInAppContentBlockPlaceholders()
        idleThreads()
        val carousel = Exponea.getInAppContentBlocksCarousel(
            ApplicationProvider.getApplicationContext(),
            "placeholder_1"
        )
        assertNotNull(carousel)
        var notifiedPlaceholderId: String? = null
        var notifiedContentBlock: InAppContentBlock? = null
        carousel.behaviourCallback = object : EmptyCarouselBehaviourCallback() {
            override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
                notifiedPlaceholderId = placeholderId
                notifiedContentBlock = contentBlock
            }
        }
        var createdCbView: InAppContentBlockPlaceholderView? = null
        carousel.viewController.contentBlockCarouselAdapter = ContentBlockCarouselAdapter(
            placeholderId = "placeholder_1",
            onPlaceholderCreated = {
                carousel.viewController.modifyPlaceholderBehaviour(it)
                createdCbView = it
            }
        )
        carousel.reload()
        idleThreads()
        val cbViewHolder = carousel.viewController.contentBlockCarouselAdapter.createViewHolder(carousel, 0)
        cbViewHolder.updateContent(carousel.getShownContentBlock())
        assertNotNull(createdCbView)
        createdCbView?.controller?.loadContent(false)
        val manualCloseUrl = createdCbView?.controller?.assignedHtmlContent?.actions?.find {
            it.actionType == HtmlActionType.CLOSE
        }
        createdCbView?.controller?.onUrlClick(manualCloseUrl?.actionUrl!!)
        assertEquals("placeholder_1", notifiedPlaceholderId)
        assertNotNull(notifiedContentBlock)
        assertEquals("id1", notifiedContentBlock?.id)
    }

    private fun prepareContentBlockMessages(messages: ArrayList<InAppContentBlock>) {
        every {
            anyConstructed<FetchManagerImpl>().fetchStaticInAppContentBlocks(any(), any(), any())
        } answers {
            arg<(Result<ArrayList<InAppContentBlock>?>) -> Unit>(1).invoke(Result(true, messages))
        }
        every {
            anyConstructed<InAppContentBlockManagerImpl>().loadInAppContentBlockPlaceholders()
        } answers {
            callOriginal()
        }
    }

    private fun initSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialProject = ExponeaProject("https://base-url.com", "project-token", "Token auth")
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, ExponeaConfiguration(
            baseURL = initialProject.baseUrl,
            projectToken = initialProject.projectToken,
            authorization = initialProject.authorization)
        )
    }
}

open class EmptyCarouselBehaviourCallback(
    override val overrideDefaultBehavior: Boolean = false,
    override val trackActions: Boolean = true
) : ContentBlockCarouselCallback {
    override fun onMessageShown(placeholderId: String, contentBlock: InAppContentBlock, index: Int, count: Int) {
        // nothing to do
    }

    override fun onMessagesChanged(count: Int, messages: List<InAppContentBlock>) {
        // nothing to do
    }

    override fun onNoMessageFound(placeholderId: String) {
        // nothing to do
    }

    override fun onError(placeholderId: String, contentBlock: InAppContentBlock?, errorMessage: String) {
        // nothing to do
    }

    override fun onCloseClicked(placeholderId: String, contentBlock: InAppContentBlock) {
        // nothing to do
    }

    override fun onActionClicked(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: InAppContentBlockAction
    ) {
        // nothing to do
    }

    override fun onHeightUpdate(placeholderId: String, height: Int) {
        // nothing to do
    }
}
