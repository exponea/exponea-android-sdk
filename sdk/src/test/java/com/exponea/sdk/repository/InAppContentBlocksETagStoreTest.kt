package com.exponea.sdk.repository

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlocksETagStoreTest {

    @Test
    fun `should store retrieve and remove etag in memory`() {
        val store = VolatileInAppContentBlocksETagStore()

        store.store("cache-key", "\"etag-v1\"")

        assertEquals("\"etag-v1\"", store.retrieve("cache-key"))

        store.remove("cache-key")

        assertNull(store.retrieve("cache-key"))
    }

    @Test
    fun `should clear all stored etags from memory`() {
        val store = VolatileInAppContentBlocksETagStore()

        store.store("first-key", "\"etag-v1\"")
        store.store("second-key", "\"etag-v2\"")

        store.clearAll()

        assertNull(store.retrieve("first-key"))
        assertNull(store.retrieve("second-key"))
    }

    @Test
    fun `should ignore empty keys`() {
        val store = VolatileInAppContentBlocksETagStore()

        store.store("", "\"etag-v1\"")

        assertNull(store.retrieve(""))
    }
}
