package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.testutil.assertEqualsIgnoreOrder
import com.google.gson.Gson
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppInboxCacheImplTest {

    companion object {
        public fun buildMessage(
            id: String,
            read: Boolean = true,
            received: Double = System.currentTimeMillis().toDouble()
        ): MessageItem {
            return MessageItem(
                id = id,
                rawType = "push",
                read = read,
                receivedTime = received,
                rawContent = mapOf(
                    "attributes" to mapOf(
                        "sent_timestamp" to received
                    )
                )
            )
        }
    }

    private lateinit var cache: AppInboxCache

    @Before
    fun before() {
        cache = AppInboxCacheImpl(ApplicationProvider.getApplicationContext(), Gson())
    }

    @After
    fun after() {
        cache.clear()
    }

    @Test
    fun `should store empty messages`() {
        cache.setMessages(arrayListOf())
        assertEqualsIgnoreOrder(arrayListOf(), cache.getMessages())
    }

    @Test
    fun `should store empty token`() {
        cache.setSyncToken(null)
        assertNull(cache.getSyncToken())
    }

    @Test
    fun `should store messages`() {
        val msg1 = buildMessage("id1")
        val msg2 = buildMessage("id2")
        cache.setMessages(
            arrayListOf(msg1, msg2)
        )
        assertEqualsIgnoreOrder(
            arrayListOf(msg1, msg2),
            cache.getMessages()
        )
    }

    @Test
    fun `should store sync token`() {
        cache.setSyncToken("some token")
        assertEquals("some token", cache.getSyncToken())
    }

    @Test
    fun `should overwrite old stored messages`() {
        val msg1 = buildMessage("id1")
        val msg2 = buildMessage("id2")
        cache.setMessages(arrayListOf(msg1))
        assertEqualsIgnoreOrder(arrayListOf(msg1), cache.getMessages())
        cache.setMessages(arrayListOf(msg2))
        assertEqualsIgnoreOrder(arrayListOf(msg2), cache.getMessages())
    }

    @Test
    fun `should overwrite old stored token`() {
        cache.setSyncToken("token1")
        assertEquals("token1", cache.getSyncToken())
        cache.setSyncToken("token2")
        assertEquals("token2", cache.getSyncToken())
    }

    @Test
    fun `should add new messages`() {
        val msg1 = buildMessage("id1")
        val msg2 = buildMessage("id2")
        cache.setMessages(arrayListOf(msg1))
        assertEqualsIgnoreOrder(arrayListOf(msg1), cache.getMessages())
        cache.addMessages(arrayListOf(msg2))
        assertEqualsIgnoreOrder(arrayListOf(msg1, msg2), cache.getMessages())
    }

    @Test
    fun `should update messages`() {
        val msg1Unread = buildMessage("id1", read = false)
        val msg1Read = buildMessage("id1", read = true)
        cache.setMessages(arrayListOf(msg1Unread))
        assertEqualsIgnoreOrder(arrayListOf(msg1Unread), cache.getMessages())
        cache.addMessages(arrayListOf(msg1Read))
        assertEqualsIgnoreOrder(arrayListOf(msg1Read), cache.getMessages())
    }

    @Test
    fun `should sort messages automatically latest first`() {
        val baseTime = System.currentTimeMillis()
        val unsortedMessages = arrayListOf(
            buildMessage("id1", received = (baseTime - 20).toDouble()),
            buildMessage("id2", received = (baseTime - 10).toDouble()),
            buildMessage("id3", received = (baseTime - 3).toDouble())
        )
        cache.setMessages(unsortedMessages)
        val sortedMessages = cache.getMessages()
        assertEquals(3, sortedMessages.size)
        assertEquals("id3", sortedMessages[0].id)
        assertEquals("id2", sortedMessages[1].id)
        assertEquals("id1", sortedMessages[2].id)
    }

    @Test
    fun `should sort messages automatically latest first after add`() {
        val baseTime = System.currentTimeMillis()
        val unsortedMessages = arrayListOf(
            buildMessage("id1", received = (baseTime - 20).toDouble()),
            buildMessage("id2", received = (baseTime - 10).toDouble())
        )
        cache.setMessages(unsortedMessages)
        val sortedMessages = cache.getMessages()
        assertEquals(2, sortedMessages.size)
        assertEquals("id2", sortedMessages[0].id)
        assertEquals("id1", sortedMessages[1].id)
        cache.addMessages(arrayListOf(buildMessage("id3", received = (baseTime - 3).toDouble())))
        val afterUpdateMessages = cache.getMessages()
        assertEquals(3, afterUpdateMessages.size)
        assertEquals("id3", afterUpdateMessages[0].id)
        assertEquals("id2", afterUpdateMessages[1].id)
        assertEquals("id1", afterUpdateMessages[2].id)
    }

    @Test
    fun `should sort messages automatically latest first after update`() {
        val baseTime = System.currentTimeMillis()
        val unsortedMessages = arrayListOf(
            buildMessage("id1", received = (baseTime - 20).toDouble()),
            buildMessage("id2", received = (baseTime - 10).toDouble()),
            buildMessage("id3", received = (baseTime - 3).toDouble())
        )
        cache.setMessages(unsortedMessages)
        val sortedMessages = cache.getMessages()
        assertEquals(3, sortedMessages.size)
        assertEquals("id3", sortedMessages[0].id)
        assertEquals("id2", sortedMessages[1].id)
        assertEquals("id1", sortedMessages[2].id)
        cache.addMessages(arrayListOf(buildMessage("id2", received = (baseTime - 1).toDouble())))
        val afterUpdateMessages = cache.getMessages()
        assertEquals(3, afterUpdateMessages.size)
        assertEquals("id2", afterUpdateMessages[0].id)
        assertEquals("id3", afterUpdateMessages[1].id)
        assertEquals("id1", afterUpdateMessages[2].id)
    }

    @Test
    fun `should get no messages when empty`() {
        assertEqualsIgnoreOrder(arrayListOf(), cache.getMessages())
    }

    @Test
    fun `should get no token when empty`() {
        assertNull(cache.getSyncToken())
    }

    @Test
    fun `should get no messages when file is corrupted`() {
        File(
            ApplicationProvider.getApplicationContext<Context>().filesDir,
            AppInboxCacheImpl.FILENAME
        ).writeText("{{{")
        assertEqualsIgnoreOrder(arrayListOf(), cache.getMessages())
    }

    @Test
    fun `should get no token when file is corrupted`() {
        File(
            ApplicationProvider.getApplicationContext<Context>().filesDir,
            AppInboxCacheImpl.FILENAME
        ).writeText("{{{")
        assertNull(cache.getSyncToken())
    }
}
