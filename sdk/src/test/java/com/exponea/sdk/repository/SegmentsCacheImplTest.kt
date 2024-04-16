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
        cache.set(SegmentTest.getSegmentsData(
            customerIds = hashMapOf(),
            data = SegmentationCategories()
        ))
        assertEquals(SegmentTest.getSegmentsData(
            customerIds = hashMapOf(),
            data = SegmentationCategories()
        ), cache.get())
    }

    @Test
    fun `should store segments`() {
        cache.set(
            SegmentTest.getSegmentsData()
        )
        assertEquals(
            SegmentTest.getSegmentsData(),
            cache.get()
        )
    }

    @Test
    fun `should overwrite old stored segments`() {
        cache.set(SegmentTest.getSegmentsData())
        assertEquals(SegmentTest.getSegmentsData(), cache.get())
        cache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "val")))
        assertEquals(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "val")), cache.get())
    }

    @Test
    fun `should get no segments when empty`() {
        assertEquals(null, cache.get())
    }

    @Test
    fun `should get no segments when file is corrupted`() {
        File(
            ApplicationProvider.getApplicationContext<Context>().cacheDir,
            SegmentsCacheImpl.SEGMENTS_FILENAME
        ).writeText("{{{")
        assertEquals(null, cache.get())
    }
}
