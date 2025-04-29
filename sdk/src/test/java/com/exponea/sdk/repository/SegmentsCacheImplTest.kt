package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationCategories
import com.google.gson.Gson
import java.io.File
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SegmentsCacheImplTest {
    private lateinit var cache: SegmentsCache

    @Before
    fun before() {
        cache = SegmentsCacheImpl(ApplicationProvider.getApplicationContext(), Gson())
    }

    @After
    fun after() {
        cache.clear()
    }

    @Test
    fun `should store empty segments`() {
        val updateMillis = System.currentTimeMillis()
        cache.set(SegmentTest.getSegmentsData(
            customerIds = hashMapOf(),
            data = SegmentationCategories(),
            updateMillis = updateMillis
        ))
        assertEquals(SegmentTest.getSegmentsData(
            customerIds = hashMapOf(),
            data = SegmentationCategories(),
            updateMillis = updateMillis
        ), cache.get())
    }

    @Test
    fun `should store segments`() {
        val segmentsToStore = SegmentTest.getSegmentsData()
        cache.set(
            segmentsToStore
        )
        assertEquals(
            segmentsToStore,
            cache.get()
        )
    }

    @Test
    fun `should overwrite old stored segments`() {
        val oldSegmentsToStore = SegmentTest.getSegmentsData()
        cache.set(oldSegmentsToStore)
        assertEquals(oldSegmentsToStore, cache.get())
        val newSegmentsToStore = SegmentTest.buildSegmentDataWithData(mapOf("prop" to "val"))
        cache.set(newSegmentsToStore)
        assertEquals(newSegmentsToStore, cache.get())
    }

    @Test
    fun `should get no segments when empty`() {
        assertEquals(null, cache.get())
    }

    @Test
    fun `should get no segments when file is corrupted`() {
        File(
            ApplicationProvider.getApplicationContext<Context>().filesDir,
            SegmentsCacheImpl.SEGMENTS_FILENAME
        ).writeText("{{{")
        assertEquals(null, cache.get())
    }
}
