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
import com.exponea.sdk.models.ContentBlockSelector
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.Result
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.model.EventType
import com.exponea.sdk.testutil.ExponeaSDKTest
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
