package com.exponea.sdk.manager

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.InAppMessageBitmapCache
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.testutil.waitForIt
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageManagerImplTest {
    private lateinit var fetchManager: FetchManager
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var messagesCache: InAppMessagesCache
    private lateinit var bitmapCache: InAppMessageBitmapCache
    private lateinit var manager: InAppMessageManager

    @Before
    fun before() {
        fetchManager = mockk()
        messagesCache = mockk()
        every { messagesCache.set(any()) } just Runs
        bitmapCache = mockk()
        every { bitmapCache.has(any()) } returns false
        every { bitmapCache.preload(any(), any()) } just Runs
        every { bitmapCache.clearExcept(any()) } just Runs
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns CustomerIds()
        manager = InAppMessageManagerImpl(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(),
            customerIdsRepository,
            messagesCache,
            fetchManager,
            bitmapCache
        )
    }

    @After
    fun after() {
        unmockkAll()
    }

    @Test
    fun `should gracefully fail to preload with fetch error`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            lastArg<(Result<FetchError>) -> Unit>().invoke(Result(false, FetchError(null, "error")))
        }
        waitForIt {
            manager.preload { result ->
                it.assertTrue(result.isFailure)
                verify(exactly = 0) { messagesCache.set(any()) }
                it()
            }
        }
    }

    @Test
    fun `should preload messages`() {
        every { fetchManager.fetchInAppMessages(any(), any(), any(), any()) } answers {
            thirdArg<(Result<List<InAppMessage>>) -> Unit>().invoke(
                Result(true, arrayListOf(InAppMessageTest.getInAppMessage()))
            )
        }
        waitForIt {
            manager.preload { result ->
                it.assertTrue(result.isSuccess)
                verify(exactly = 1) { messagesCache.set(arrayListOf(InAppMessageTest.getInAppMessage())) }
                it()
            }
        }
    }

    @Test
    fun `should get null if no messages available`() {
        every { messagesCache.get() } returns arrayListOf()
        assertNull(manager.getRandom())
    }

    @Test
    fun `should get message if both message and bitmap available`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(),
            InAppMessageTest.getInAppMessage()
        )
        every { bitmapCache.has(any()) } returns true
        assertEquals(InAppMessageTest.getInAppMessage(), manager.getRandom())
    }

    @Test
    fun `should not get message if bitmap is not available`() {
        every { messagesCache.get() } returns arrayListOf(
            InAppMessageTest.getInAppMessage(),
            InAppMessageTest.getInAppMessage()
        )
        assertEquals(null, manager.getRandom())
    }
}
