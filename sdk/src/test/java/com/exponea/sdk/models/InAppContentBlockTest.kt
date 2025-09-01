package com.exponea.sdk.models

import com.exponea.sdk.manager.InAppContentBlockManagerImplTest
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

internal class InAppContentBlockTest {
    @Test
    fun `should have fresh content for Static-InAppContentBlock`() {
        val staticMessage = InAppContentBlockManagerImplTest.buildMessage(
            "id1", type = "html"
        )
        assertTrue(staticMessage.hasFreshContent())
    }

    @Test
    fun `should have fresh content for fresh Personalized-InAppContentBlock`() {
        val message = InAppContentBlockManagerImplTest.buildMessage(
            "id1",
            dateFilter = null
        )
        val content = InAppContentBlockManagerImplTest.buildMessageData(
            "id1",
            ttl = 5
        ).apply {
            // this is set by fetch manager
            loadedAt = Date()
        }
        message.personalizedData = content
        assertTrue(message.hasFreshContent())
    }

    @Test
    fun `should NOT have fresh content for old Personalized-InAppContentBlock`() {
        val message = InAppContentBlockManagerImplTest.buildMessage(
            "id1",
            dateFilter = null
        )
        val content = InAppContentBlockManagerImplTest.buildMessageData(
            "id1",
            ttl = 5
        ).apply {
            // this is set by fetch manager
            loadedAt = Date(System.currentTimeMillis() - (20 * 1000))
        }
        message.personalizedData = content
        assertFalse(message.hasFreshContent())
    }
}
