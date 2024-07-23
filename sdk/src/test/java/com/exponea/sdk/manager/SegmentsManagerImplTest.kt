package com.exponea.sdk.manager

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.models.SegmentationData
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.SegmentsCache
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.testutil.reset
import com.exponea.sdk.testutil.resetVerifyMockkCount
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.currentTimeSeconds
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SegmentsManagerImplTest {
    private lateinit var fetchManager: FetchManager
    private lateinit var customerIdsRepository: CustomerIdsRepository
    private lateinit var projectFactory: ExponeaProjectFactory
    private lateinit var segmentsCache: SegmentsCache
    private lateinit var segmentsManager: SegmentsManager

    @Before
    fun before() {
        fetchManager = mockk()
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(
                Result(true, SegmentTest.buildSingleSegmentWithData(emptyMap()))
            )
        }
        every { fetchManager.linkCustomerIdsSync(any(), any()) } answers {
            Result<Any?>(true, null)
        }
        customerIdsRepository = mockk()
        every { customerIdsRepository.get() } returns SegmentTest.getCustomerIds()
        val configuration = ExponeaConfiguration(projectToken = "mock-token")
        projectFactory = ExponeaProjectFactory(ApplicationProvider.getApplicationContext(), configuration)
        segmentsCache = mockk()
        var segmentsData: SegmentationData? = null
        every { segmentsCache.set(any()) } answers {
            segmentsData = firstArg()
        }
        every { segmentsCache.get() } answers {
            segmentsData
        }
        every { segmentsCache.clear() } answers {
            segmentsData = null
            true
        }
        segmentsManager = SegmentsManagerImpl(
            fetchManager = fetchManager,
            projectFactory = projectFactory,
            customerIdsRepository = customerIdsRepository,
            segmentsCache = segmentsCache
        )
    }

    @Before
    fun activateSegmentFlow() {
        Exponea.reset()
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                // just be set
            }
        })
    }

    @After
    fun deactivateSegmentFlow() {
        Exponea.segmentationDataCallbacks.clear()
    }

    @Test
    fun `should not store segments on error`() {
        segmentsCache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "mock-val")))
        segmentsCache.resetVerifyMockkCount()
        waitForIt { done ->
            every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
                arg<(Result<FetchError>) -> Unit>(3).invoke(Result(false, FetchError(null, "error")))
                done()
            }
            segmentsManager.onEventUploaded(buildExportedEvent())
        }
        verify(exactly = 0) { segmentsCache.set(any()) }
        val cachedData = segmentsCache.get()
        assertNotNull(cachedData)
        val cachedSegments = cachedData.segmentations["discovery"]
        assertNotNull(cachedSegments)
        assertEquals(1, cachedSegments.size)
        assertEquals("mock-val", cachedSegments.first()["prop"])
    }

    @Test
    fun `should store empty segments on success`() {
        segmentsCache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "mock-val")))
        segmentsCache.resetVerifyMockkCount()
        waitForIt(50000) { done ->
            every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
                arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(
                    Result(true, SegmentationCategories())
                )
                done()
            }
            segmentsManager.onEventUploaded(buildExportedEvent())
        }
        verify(exactly = 1) { segmentsCache.set(any()) }
        val cachedData = segmentsCache.get()
        assertNotNull(cachedData)
        assertEquals(0, cachedData.segmentations.size)
    }

    @Test
    fun `should store some segments on success`() {
        segmentsCache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "mock-val")))
        segmentsCache.resetVerifyMockkCount()
        waitForIt { done ->
            every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
                arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                    true,
                    SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "another-mock-val"))
                ))
                done()
            }
            segmentsManager.onEventUploaded(buildExportedEvent())
        }
        verify(exactly = 1) { segmentsCache.set(any()) }
        val cachedData = segmentsCache.get()
        assertNotNull(cachedData)
        val discoverySegments = cachedData.segmentations["discovery"]
        assertNotNull(discoverySegments)
        assertEquals(1, discoverySegments.size)
        assertEquals("another-mock-val", discoverySegments.first()["prop"])
    }

    @Test
    fun `should not override same segments on success`() {
        segmentsCache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "mock-val")))
        segmentsCache.resetVerifyMockkCount()
        waitForIt { done ->
            every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
                arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                    true,
                    SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
                ))
                done()
            }
            segmentsManager.onEventUploaded(buildExportedEvent())
        }
        verify(exactly = 0) { segmentsCache.set(any()) }
        val cachedData = segmentsCache.get()
        assertNotNull(cachedData)
        val discoverySegments = cachedData.segmentations["discovery"]
        assertNotNull(discoverySegments)
        assertEquals(1, discoverySegments.size)
        assertEquals("mock-val", discoverySegments.first()["prop"])
    }

    @Test
    fun `should notify callback for new segments on success`() {
        segmentsCache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "mock-val")))
        segmentsCache.resetVerifyMockkCount()
        var newSegmentsFound: List<Segment>? = null
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                newSegmentsFound = segments
            }
        })
        waitForIt { done ->
            every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
                arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                    true,
                    SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "another-mock-val"))
                ))
                done()
            }
            segmentsManager.onEventUploaded(buildExportedEvent())
        }
        verify(exactly = 1) { segmentsCache.set(any()) }
        assertNotNull(newSegmentsFound)
        assertEquals(1, newSegmentsFound?.size)
        assertEquals("another-mock-val", newSegmentsFound?.first()?.get("prop"))
    }

    @Test
    fun `should not notify callback for same segments on success`() {
        segmentsCache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "mock-val")))
        segmentsCache.resetVerifyMockkCount()
        var newSegmentsFound: List<Segment>? = null
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                newSegmentsFound = segments
            }
        })
        waitForIt { done ->
            every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
                arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                    true,
                    SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
                ))
                done()
            }
            segmentsManager.onEventUploaded(buildExportedEvent())
        }
        verify(exactly = 0) { segmentsCache.set(any()) }
        assertNull(newSegmentsFound)
    }

    @Test
    fun `should not notify callback on error`() {
        segmentsCache.set(SegmentTest.buildSegmentDataWithData(mapOf("prop" to "mock-val")))
        segmentsCache.resetVerifyMockkCount()
        var newSegmentsFound: List<Segment>? = null
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                newSegmentsFound = segments
            }
        })
        waitForIt { done ->
            every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
                arg<(Result<FetchError>) -> Unit>(3).invoke(Result(false, FetchError(null, "error")))
                done()
            }
            segmentsManager.onEventUploaded(buildExportedEvent())
        }
        verify(exactly = 0) { segmentsCache.set(any()) }
        assertNull(newSegmentsFound)
    }

    @Test
    fun `should not reload segments for inactive callback - track event`() {
        Exponea.segmentationDataCallbacks.clear()
        segmentsManager.onEventUploaded(buildExportedEvent())
        verify(exactly = 0) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should not reload segments for inactive callback - callback changed`() {
        Exponea.segmentationDataCallbacks.clear()
        segmentsManager.reload()
        verify(exactly = 0) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should not reload segments for inactive callback - reload`() {
        Exponea.segmentationDataCallbacks.clear()
        segmentsManager.reload()
        verify(exactly = 0) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should fetch segments with debounce`() {
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        for (i in 0..10) {
            segmentsManager.reload()
            segmentsManager.onEventUploaded(buildExportedEvent())
            segmentsManager.onCallbackAdded(object : SegmentationDataCallback() {
                override val exposingCategory = "discovery"
                override val includeFirstLoad = false
                override fun onNewData(segments: List<Segment>) {
                    // nothing
                }
            })
        }
        verify(exactly = 0) { fetchManager.fetchSegments(any(), any(), any(), any()) }
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should call linking of customer IDs because of no data`() {
        segmentsCache.clear()
        segmentsManager.reload()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 1) { fetchManager.linkCustomerIdsSync(any(), any()) }
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should call linking of customer IDs because of empty customer IDs`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = hashMapOf()
        ))
        segmentsManager.reload()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 1) { fetchManager.linkCustomerIdsSync(any(), any()) }
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should call linking of customer IDs because of obsolete customer IDs`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = hashMapOf("cookie" to "old-cookie", "registered" to "old-registered")
        ))
        segmentsManager.reload()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 1) { fetchManager.linkCustomerIdsSync(any(), any()) }
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should not call linking of customer IDs because of same customer IDs`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap()
        ))
        segmentsManager.reload()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 0) { fetchManager.linkCustomerIdsSync(any(), any()) }
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should not call linking of customer IDs because of no external customer IDs`() {
        every { customerIdsRepository.get() } returns SegmentTest.getCustomerIds(hashMapOf(
            "cookie" to "mock-cookie"
        ))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = hashMapOf()
        ))
        segmentsManager.reload()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 0) { fetchManager.linkCustomerIdsSync(any(), any()) }
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should notify callback with correct exposingCategory`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        val called = CountDownLatch(1)
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                assertEquals(1, segments.size)
                assertEquals("mock-val", segments[0]["prop"])
                called.countDown()
            }
        })
        segmentsManager.reload()
        assertTrue(called.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should not notify callback with different exposingCategory`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        segmentsCache.resetVerifyMockkCount()
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        val called = CountDownLatch(1)
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "another-interest"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                called.countDown()
            }
        })
        segmentsManager.reload()
        assertFalse(called.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000, TimeUnit.MILLISECONDS))
        // callback has not been called but verify that process has been invoked
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
        verify(exactly = 1) { segmentsCache.set(any()) }
    }

    @Test
    fun `should notify all callbacks with correct exposingCategory`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        val callbacksCount = 5
        val called = CountDownLatch(callbacksCount)
        for (i in 1..callbacksCount) {
            Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
                override val exposingCategory = "discovery"
                override val includeFirstLoad = false
                override fun onNewData(segments: List<Segment>) {
                    assertEquals(1, segments.size)
                    assertEquals("mock-val", segments[0]["prop"])
                    called.countDown()
                }
            })
        }
        segmentsManager.reload()
        assertTrue(called.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should notify only callbacks with correct exposingCategory`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        val interestInCallbacksCount = 5
        val nonInterestInCallbacksCount = 5
        val called = CountDownLatch(interestInCallbacksCount)
        for (i in 1..nonInterestInCallbacksCount) {
            Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
                override val exposingCategory = "another-than-discovery"
                override val includeFirstLoad = false
                override fun onNewData(segments: List<Segment>) {
                    fail("This segment callback has not to be called")
                }
            })
        }
        for (i in 1..interestInCallbacksCount) {
            Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
                override val exposingCategory = "discovery"
                override val includeFirstLoad = false
                override fun onNewData(segments: List<Segment>) {
                    assertEquals(1, segments.size)
                    assertEquals("mock-val", segments[0]["prop"])
                    called.countDown()
                }
            })
        }
        segmentsManager.reload()
        assertTrue(called.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should stop fetch for unregistered callback`() {
        Exponea.segmentationDataCallbacks.clear()
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        segmentsCache.resetVerifyMockkCount()
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        val segmentCallback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                fail("This segment callback has not to be called as is unregistered")
            }
        }
        Exponea.registerSegmentationDataCallback(segmentCallback)
        Exponea.unregisterSegmentationDataCallback(segmentCallback)
        assertEquals(0, Exponea.segmentationDataCallbacks.size)
        segmentsManager.reload()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 0) { fetchManager.fetchSegments(any(), any(), any(), any()) }
        verify(exactly = 0) { segmentsCache.set(any()) }
    }

    @Test
    fun `should stop fetch for self-unregistered callback`() {
        Exponea.segmentationDataCallbacks.clear()
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        segmentsCache.resetVerifyMockkCount()
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        val segmentCallback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                fail("This segment callback has not to be called as is unregistered")
            }
        }
        Exponea.registerSegmentationDataCallback(segmentCallback)
        segmentCallback.unregister()
        assertEquals(0, Exponea.segmentationDataCallbacks.size)
        segmentsManager.reload()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 0) { fetchManager.fetchSegments(any(), any(), any(), any()) }
        verify(exactly = 0) { segmentsCache.set(any()) }
    }

    @Test
    fun `should notify all callbacks in registered order`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        var orderFlag = ""
        val callbacksCount = 5
        val called = CountDownLatch(callbacksCount)
        for (i in 1..callbacksCount) {
            Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
                override val exposingCategory = "discovery"
                override val includeFirstLoad = false
                override fun onNewData(segments: List<Segment>) {
                    orderFlag += i
                    assertEquals(1, segments.size)
                    assertEquals("mock-val", segments[0]["prop"])
                    called.countDown()
                }
            })
        }
        segmentsManager.reload()
        assertTrue(called.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
        assertEquals("12345", orderFlag)
    }

    @Test
    fun `should notify all callbacks also in case of exception`() {
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = SegmentationCategories()
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        var orderFlag = ""
        val callbacksCount = 5
        val called = CountDownLatch(callbacksCount)
        for (i in 1..callbacksCount) {
            Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
                override val exposingCategory = "discovery"
                override val includeFirstLoad = false
                override fun onNewData(segments: List<Segment>) {
                    called.countDown()
                    if (i == 4) {
                        throw RuntimeException("Sorry, something went wrong")
                    }
                    orderFlag += i
                }
            })
        }
        segmentsManager.reload()
        assertTrue(called.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
        assertEquals("1235", orderFlag)
    }

    @Test
    fun `should not notify callback for same data if includeFirstLoad is false`() {
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        val called = CountDownLatch(1)
        val callback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                called.countDown()
            }
        }
        Exponea.registerSegmentationDataCallback(callback)
        segmentsManager.onCallbackAdded(callback)
        assertFalse(called.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should fetch segments while SDK init if any includeFirstLoad is true`() {
        Exponea.segmentationDataCallbacks.clear()
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                // nothing
            }
        })
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        segmentsManager.onSdkInit()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should not fetch segments while SDK init if all includeFirstLoad are false`() {
        Exponea.segmentationDataCallbacks.clear()
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                // nothing
            }
        })
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        segmentsManager.onSdkInit()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 0) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should fetch segments if new callback has includeFirstLoad as true`() {
        Exponea.segmentationDataCallbacks.clear()
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        Exponea.reset()
        val callback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                // nothing
            }
        }
        Exponea.registerSegmentationDataCallback(callback)
        segmentsManager.onCallbackAdded(callback)
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should not fetch segments if new callback has includeFirstLoad as false`() {
        Exponea.segmentationDataCallbacks.clear()
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        Exponea.reset()
        val callback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                // nothing
            }
        }
        Exponea.registerSegmentationDataCallback(callback)
        segmentsManager.onCallbackAdded(callback)
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        verify(exactly = 1) { fetchManager.fetchSegments(any(), any(), any(), any()) }
    }

    @Test
    fun `should notify callback for same data if includeFirstLoad is true - SDK init`() {
        Exponea.segmentationDataCallbacks.clear()
        val callbackNotified = CountDownLatch(1)
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                assertEquals(1, segments.size)
                assertEquals("mock-val", segments[0]["prop"])
                callbackNotified.countDown()
            }
        })
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        segmentsManager.onSdkInit()
        assertTrue(callbackNotified.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should notify same data only callback with includeFirstLoad is true - SDK init`() {
        Exponea.segmentationDataCallbacks.clear()
        var notifFlag = ""
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                notifFlag += "valid"
            }
        })
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                notifFlag += "invalid"
            }
        })
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        segmentsManager.onSdkInit()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000)
        assertEquals("valid", notifFlag)
    }

    @Test
    fun `should notify callback for same data if includeFirstLoad is true but only once - SDK init`() {
        Exponea.segmentationDataCallbacks.clear()
        var notifCount = 0
        Exponea.registerSegmentationDataCallback(object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                notifCount += 1
            }
        })
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        segmentsManager.onSdkInit()
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000)
        assertEquals(1, notifCount)
        // SDK cannot be initialized twice so only onEventUploaded could occur
        segmentsManager.onEventUploaded(buildExportedEvent())
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000)
        assertEquals(1, notifCount)
    }

    @Test
    fun `should notify callback for same data if includeFirstLoad is true - callback registration`() {
        Exponea.segmentationDataCallbacks.clear()
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        Exponea.reset()
        val callbackNotified = CountDownLatch(1)
        val callback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                assertEquals(1, segments.size)
                assertEquals("mock-val", segments[0]["prop"])
                callbackNotified.countDown()
            }
        }
        Exponea.registerSegmentationDataCallback(callback)
        segmentsManager.onCallbackAdded(callback)
        assertTrue(callbackNotified.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should notify same data only callback with includeFirstLoad is true - callback registration`() {
        Exponea.segmentationDataCallbacks.clear()
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        Exponea.reset()
        var notifFlag = ""
        val validCallback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                notifFlag += "valid"
            }
        }
        Exponea.registerSegmentationDataCallback(validCallback)
        segmentsManager.onCallbackAdded(validCallback)
        val invalidCallback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                notifFlag += "invalid"
            }
        }
        Exponea.registerSegmentationDataCallback(invalidCallback)
        segmentsManager.onCallbackAdded(invalidCallback)
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000)
        assertEquals("valid", notifFlag)
    }

    @Test
    fun `should notify callback for same data if includeFirstLoad is true but only once - callback registration`() {
        Exponea.segmentationDataCallbacks.clear()
        val segmentationsData = SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = segmentationsData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                segmentationsData
            ))
        }
        Exponea.reset()
        var notifCount = 0
        val callback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                notifCount += 1
            }
        }
        Exponea.registerSegmentationDataCallback(callback)
        segmentsManager.onCallbackAdded(callback)
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500)
        assertEquals(1, notifCount)
        // callback could be registered twice so fetch is expected to be called twice
        // but we need to ensure that next Event upload will not trigger callback second time
        segmentsManager.onEventUploaded(buildExportedEvent())
        Thread.sleep(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 2000)
        assertEquals(1, notifCount)
    }

    @Test
    fun `should notify callback for empty category if includeFirstLoad is true - discovery`() {
        Exponea.segmentationDataCallbacks.clear()
        val emptyData = SegmentationCategories()
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = emptyData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                emptyData
            ))
        }
        Exponea.reset()
        val callbackNotified = CountDownLatch(1)
        val callback = object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                assertEquals(0, segments.size)
                callbackNotified.countDown()
            }
        }
        Exponea.registerSegmentationDataCallback(callback)
        segmentsManager.onCallbackAdded(callback)
        assertTrue(callbackNotified.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `should notify callback for empty category if includeFirstLoad is true - custom_future_category`() {
        Exponea.segmentationDataCallbacks.clear()
        val emptyData = SegmentationCategories()
        segmentsCache.set(SegmentTest.getSegmentsData(
            customerIds = customerIdsRepository.get().toHashMap(),
            data = emptyData
        ))
        every { fetchManager.fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                emptyData
            ))
        }
        Exponea.reset()
        val callbackNotified = CountDownLatch(1)
        val callback = object : SegmentationDataCallback() {
            override val exposingCategory = "custom_future_category"
            override val includeFirstLoad = true
            override fun onNewData(segments: List<Segment>) {
                assertEquals(0, segments.size)
                callbackNotified.countDown()
            }
        }
        Exponea.registerSegmentationDataCallback(callback)
        segmentsManager.onCallbackAdded(callback)
        assertTrue(callbackNotified.await(SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + 500, TimeUnit.MILLISECONDS))
    }

    private fun buildExportedEvent(): ExportedEvent {
        return ExportedEvent(
            id = "1234",
            tries = 0,
            projectId = "mock-proj",
            route = null,
            shouldBeSkipped = false,
            exponeaProject = null,
            type = null,
            timestamp = currentTimeSeconds(),
            age = 10.0,
            customerIds = customerIdsRepository.get().toHashMap(),
            properties = hashMapOf(),
            sdkEventType = EventType.TRACK_CUSTOMER.name
        )
    }
}
