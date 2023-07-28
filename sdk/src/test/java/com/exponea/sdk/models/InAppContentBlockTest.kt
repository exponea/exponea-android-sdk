package com.exponea.sdk.models

import com.exponea.sdk.manager.InAppContentBlocksManagerImplTest
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockTest {
    @Test
    fun `should have fresh content for Static-InAppContentBlock`() {
        val staticMessage = InAppContentBlocksManagerImplTest.buildMessage(
            "id1", type = "html"
        )
        assertTrue(staticMessage.hasFreshContent())
    }

    @Test
    fun `should have fresh content for fresh Personalized-InAppContentBlock`() {
        val message = InAppContentBlocksManagerImplTest.buildMessage(
            "id1"
        )
        val content = InAppContentBlocksManagerImplTest.buildMessageData(
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
        val message = InAppContentBlocksManagerImplTest.buildMessage(
            "id1"
        )
        val content = InAppContentBlocksManagerImplTest.buildMessageData(
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
