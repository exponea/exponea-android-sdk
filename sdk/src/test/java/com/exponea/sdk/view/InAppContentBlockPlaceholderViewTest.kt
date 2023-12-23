package com.exponea.sdk.view

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Looper.getMainLooper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.R
import com.exponea.sdk.manager.FetchManager
import com.exponea.sdk.manager.InAppContentBlockManager
import com.exponea.sdk.manager.InAppContentBlocksManagerImpl
import com.exponea.sdk.manager.InAppContentBlocksManagerImplTest.Companion.buildHtmlMessageContent
import com.exponea.sdk.manager.InAppContentBlocksManagerImplTest.Companion.buildMessage
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockCallback
import com.exponea.sdk.models.InAppContentBlockDisplayState
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
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
import kotlin.test.assertFalse
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
    private lateinit var bitmapCache: BitmapCache
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
        every { bitmapCache.get(any()) } returns
            BitmapFactory.decodeResource(context.resources, R.drawable.message_inbox_button)
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
        inAppContentBlockManager = InAppContentBlocksManagerImpl(
            displayStateRepository = displayStateRepository,
            fetchManager = fetchManager,
            projectFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
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
