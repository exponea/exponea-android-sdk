package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessageTest
import com.google.gson.Gson
import java.io.File
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessagesCacheImplTest {
    private lateinit var cache: InAppMessagesCache

    @Before
    fun before() {
        cache = InAppMessagesCacheImpl(ApplicationProvider.getApplicationContext(), Gson())
    }

    @After
    fun after() {
        cache.clear()
    }

    @Test
    fun `should store empty messages`() {
        cache.set(arrayListOf())
        assertEquals(arrayListOf(), cache.get())
    }

    @Test
    fun `should store messages`() {
        cache.set(arrayListOf(
            InAppMessageTest.buildInAppMessageWithRichstyle("id1"),
            InAppMessageTest.buildInAppMessageWithRichstyle("id2"),
            InAppMessageTest.buildInAppMessageWithoutRichstyle("id3")
        ))
        assertEquals(
            arrayListOf(
                InAppMessageTest.buildInAppMessageWithRichstyle("id1"),
                InAppMessageTest.buildInAppMessageWithRichstyle("id2"),
                InAppMessageTest.buildInAppMessageWithoutRichstyle("id3")
            ),
            cache.get()
        )
    }

    @Test
    fun `should overwrite old stored messages`() {
        cache.set(arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle("id1")))
        assertEquals(arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle("id1")), cache.get())
        cache.set(arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle("id2")))
        assertEquals(arrayListOf(InAppMessageTest.buildInAppMessageWithRichstyle("id2")), cache.get())
    }

    @Test
    fun `should get no messages when empty`() {
        assertEquals(arrayListOf(), cache.get())
    }

    @Test
    fun `should get no messages when file is corrupted`() {
        File(
            ApplicationProvider.getApplicationContext<Context>().filesDir,
            InAppMessagesCacheImpl.IN_APP_MESSAGES_FILENAME
        ).writeText("{{{")
        assertEquals(arrayListOf(), cache.get())
    }
}
