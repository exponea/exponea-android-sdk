package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildHtmlMessageContent
import com.exponea.sdk.manager.InAppContentBlockManagerImplTest.Companion.buildMessage
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.repository.HtmlNormalizedCache
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockViewControllerTest {

    private val executor = Executors.newFixedThreadPool(4)

    @After
    fun after() {
        executor.shutdownNow()
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Default)
    }

    @Test
    fun `older load finishing last should not overwrite newer assignment`() {
        val scope = CoroutineScope(executor.asCoroutineDispatcher())
        backgroundThreadDispatcher = scope
        mainThreadDispatcher = scope
        val oldBlock = buildMessage("old_block", type = "html")
        val newBlock = buildMessage("new_block", type = "html")
        val firstLoadStarted = CountDownLatch(1)
        val releaseFirstLoad = CountDownLatch(1)
        val firstLoadFinished = CountDownLatch(1)
        val loadCounter = AtomicInteger(0)
        val dataLoader = object : InAppContentBlockDataLoader {
            override fun loadContent(placeholderId: String): InAppContentBlock? {
                if (loadCounter.incrementAndGet() == 1) {
                    // simulates slow load (i.e. HTML normalization cache miss) of previously assigned block
                    firstLoadStarted.countDown()
                    releaseFirstLoad.await(5, TimeUnit.SECONDS)
                    firstLoadFinished.countDown()
                    return oldBlock
                }
                return newBlock
            }
        }
        val controller = InAppContentBlockViewController(
            placeholderId = "placeholder_1",
            config = InAppContentBlockPlaceholderConfiguration(true),
            imageCache = mockk(relaxed = true),
            fontCache = mockk(relaxed = true),
            htmlCache = mockk(relaxed = true),
            actionDispatcher = mockk(relaxed = true),
            dataLoader = dataLoader,
            behaviourCallback = mockk(relaxed = true)
        )
        controller.view = mockk<InAppContentBlockPlaceholderView>(relaxed = true)
        controller.loadContent(false)
        assertTrue(firstLoadStarted.await(5, TimeUnit.SECONDS))
        controller.loadContent(false)
        waitUntil { controller.assignedMessage?.id == newBlock.id }
        releaseFirstLoad.countDown()
        assertTrue(firstLoadFinished.await(5, TimeUnit.SECONDS))
        // let the outdated load finish its processing
        Thread.sleep(200)
        assertEquals(newBlock.id, controller.assignedMessage?.id)
    }

    @Test
    fun `outdated render should not track message as shown`() {
        // background runs inline, main thread tasks are queued and executed on demand
        val mainQueue = ArrayDeque<Runnable>()
        backgroundThreadDispatcher = CoroutineScope(Executor { it.run() }.asCoroutineDispatcher())
        mainThreadDispatcher = CoroutineScope(Executor { mainQueue.addLast(it) }.asCoroutineDispatcher())
        val oldBlock = buildMessage("old_block", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        val newBlock = buildMessage("new_block", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        val loadCounter = AtomicInteger(0)
        val dataLoader = object : InAppContentBlockDataLoader {
            override fun loadContent(placeholderId: String): InAppContentBlock? {
                return if (loadCounter.incrementAndGet() == 1) oldBlock else newBlock
            }
        }
        val actionDispatcher = mockk<InAppContentBlockActionDispatcher>(relaxed = true)
        val behaviourCallback = mockk<InAppContentBlockCallback>(relaxed = true)
        val htmlCache = mockk<HtmlNormalizedCache>(relaxed = true)
        every { htmlCache.get(any(), any()) } answers { NormalizedResult().apply { html = secondArg() } }
        val controller = InAppContentBlockViewController(
            placeholderId = "placeholder_1",
            config = InAppContentBlockPlaceholderConfiguration(true),
            imageCache = mockk(relaxed = true),
            fontCache = mockk(relaxed = true),
            htmlCache = htmlCache,
            actionDispatcher = actionDispatcher,
            dataLoader = dataLoader,
            behaviourCallback = behaviourCallback
        )
        controller.view = mockk<InAppContentBlockPlaceholderView>(relaxed = true)
        // first render is posted to main thread, but newer load is assigned before it runs
        controller.loadContent(false)
        controller.loadContent(false)
        while (mainQueue.isNotEmpty()) {
            mainQueue.removeFirst().run()
        }
        verify(exactly = 0) { actionDispatcher.onShown(any(), oldBlock) }
        verify(exactly = 0) { behaviourCallback.onMessageShown(any(), oldBlock) }
        verify(exactly = 1) { actionDispatcher.onShown("placeholder_1", newBlock) }
        verify(exactly = 1) { behaviourCallback.onMessageShown("placeholder_1", newBlock) }
    }

    @Test
    fun `render posted before clean up should not be shown nor tracked`() {
        val mainQueue = ArrayDeque<Runnable>()
        backgroundThreadDispatcher = CoroutineScope(Executor { it.run() }.asCoroutineDispatcher())
        mainThreadDispatcher = CoroutineScope(Executor { mainQueue.addLast(it) }.asCoroutineDispatcher())
        val block = buildMessage("block", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        val actionDispatcher = mockk<InAppContentBlockActionDispatcher>(relaxed = true)
        val htmlCache = mockk<HtmlNormalizedCache>(relaxed = true)
        every { htmlCache.get(any(), any()) } answers { NormalizedResult().apply { html = secondArg() } }
        val controller = InAppContentBlockViewController(
            placeholderId = "placeholder_1",
            config = InAppContentBlockPlaceholderConfiguration(true),
            imageCache = mockk(relaxed = true),
            fontCache = mockk(relaxed = true),
            htmlCache = htmlCache,
            actionDispatcher = actionDispatcher,
            dataLoader = object : InAppContentBlockDataLoader {
                override fun loadContent(placeholderId: String) = block
            },
            behaviourCallback = mockk(relaxed = true)
        )
        val view = mockk<InAppContentBlockPlaceholderView>(relaxed = true)
        controller.view = view
        controller.loadContent(false)
        // i.e. carousel view holder is recycled before render reaches main thread
        controller.cleanUp()
        while (mainQueue.isNotEmpty()) {
            mainQueue.removeFirst().run()
        }
        verify(exactly = 0) { view.showHtmlContent(any(), any()) }
        verify(exactly = 0) { view.showExistingContent(any()) }
        verify(exactly = 0) { actionDispatcher.onShown(any(), any()) }
    }

    @Test
    fun `outdated no-message render should not notify`() {
        val newBlock = buildMessage("new_block", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        val env = QueuedMainEnv(listOf(null, newBlock))
        env.controller.loadContent(false)
        env.controller.loadContent(false)
        env.drainMain()
        verify(exactly = 0) { env.behaviourCallback.onNoMessageFound(any()) }
        verify(exactly = 1) { env.actionDispatcher.onShown("placeholder_1", newBlock) }
    }

    @Test
    fun `outdated no-content render should not track`() {
        val oldBlock = buildMessage("old_block", type = "unknown")
        val newBlock = buildMessage("new_block", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        val env = QueuedMainEnv(listOf(oldBlock, newBlock))
        env.controller.loadContent(false)
        env.controller.loadContent(false)
        env.drainMain()
        verify(exactly = 0) { env.actionDispatcher.onNoContent(any(), any()) }
        verify(exactly = 0) { env.behaviourCallback.onMessageShown(any(), oldBlock) }
        verify(exactly = 1) { env.actionDispatcher.onShown("placeholder_1", newBlock) }
        verify(exactly = 1) { env.behaviourCallback.onMessageShown("placeholder_1", newBlock) }
    }

    @Test
    fun `outdated error render should not track`() {
        val invalidHtml = "invalid_html"
        val oldBlock = buildMessage("old_block", type = "html", data = mapOf("html" to invalidHtml))
        val newBlock = buildMessage("new_block", type = "html", data = mapOf("html" to buildHtmlMessageContent()))
        val env = QueuedMainEnv(listOf(oldBlock, newBlock))
        every { env.htmlCache.get(any(), any()) } answers {
            NormalizedResult().apply { html = if (secondArg<String>() == invalidHtml) null else secondArg() }
        }
        env.controller.loadContent(false)
        env.controller.loadContent(false)
        env.drainMain()
        verify(exactly = 0) { env.actionDispatcher.onError(any(), any(), any()) }
        verify(exactly = 0) { env.behaviourCallback.onError(any(), any(), any()) }
        verify(exactly = 1) { env.actionDispatcher.onShown("placeholder_1", newBlock) }
    }

    @Test
    fun `no-content render after clean up should not be shown nor tracked`() {
        val block = buildMessage("block", type = "unknown")
        val env = QueuedMainEnv(listOf(block))
        env.controller.loadContent(false)
        // i.e. carousel view holder is recycled before render reaches main thread
        env.controller.cleanUp()
        env.drainMain()
        verify(exactly = 0) { env.view.showNoContent(any()) }
        verify(exactly = 0) { env.actionDispatcher.onNoContent(any(), any()) }
        verify(exactly = 0) { env.behaviourCallback.onMessageShown(any(), any()) }
    }

    @Test
    fun `active no-content render should track assigned message`() {
        val block = buildMessage("block", type = "unknown")
        val env = QueuedMainEnv(listOf(block))
        env.controller.loadContent(false)
        env.drainMain()
        verify(exactly = 1) { env.view.showNoContent(any()) }
        verify(exactly = 1) { env.actionDispatcher.onNoContent("placeholder_1", block) }
        verify(exactly = 1) { env.behaviourCallback.onMessageShown("placeholder_1", block) }
    }

    @Test
    fun `active no-message render should notify`() {
        val env = QueuedMainEnv(listOf(null))
        env.controller.loadContent(false)
        env.drainMain()
        verify(exactly = 1) { env.view.showNoContent(any()) }
        verify(exactly = 1) { env.behaviourCallback.onNoMessageFound("placeholder_1") }
    }

    /**
     * Background runs inline, main thread tasks are queued and executed by [drainMain].
     * Data loader returns [loads] one by one, last one is repeated.
     */
    private inner class QueuedMainEnv(loads: List<InAppContentBlock?>) {
        private val mainQueue = ArrayDeque<Runnable>()
        val actionDispatcher = mockk<InAppContentBlockActionDispatcher>(relaxed = true)
        val behaviourCallback = mockk<InAppContentBlockCallback>(relaxed = true)
        val htmlCache = mockk<HtmlNormalizedCache>(relaxed = true)
        val view = mockk<InAppContentBlockPlaceholderView>(relaxed = true)
        val controller: InAppContentBlockViewController

        init {
            backgroundThreadDispatcher = CoroutineScope(Executor { it.run() }.asCoroutineDispatcher())
            mainThreadDispatcher = CoroutineScope(Executor { mainQueue.addLast(it) }.asCoroutineDispatcher())
            every { htmlCache.get(any(), any()) } answers { NormalizedResult().apply { html = secondArg() } }
            val loadCounter = AtomicInteger(0)
            controller = InAppContentBlockViewController(
                placeholderId = "placeholder_1",
                config = InAppContentBlockPlaceholderConfiguration(true),
                imageCache = mockk(relaxed = true),
                fontCache = mockk(relaxed = true),
                htmlCache = htmlCache,
                actionDispatcher = actionDispatcher,
                dataLoader = object : InAppContentBlockDataLoader {
                    override fun loadContent(placeholderId: String): InAppContentBlock? {
                        return loads[minOf(loadCounter.getAndIncrement(), loads.size - 1)]
                    }
                },
                behaviourCallback = behaviourCallback
            )
            controller.view = view
        }

        fun drainMain() {
            while (mainQueue.isNotEmpty()) {
                mainQueue.removeFirst().run()
            }
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5000
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("Condition not met in time")
            }
            Thread.sleep(10)
        }
    }
}
