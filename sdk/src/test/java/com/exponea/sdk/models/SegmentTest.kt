package com.exponea.sdk.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test

internal class SegmentTest {
    companion object {
        const val SEGMENT_JSON = """
            {"prop1":"val1", "prop2":"2", "prop3":"true"}
        """
        const val SEGMENTATIONS_JSON = """
        {
          "discovery": [
            $SEGMENT_JSON,
            {"prop1":"valA", "prop2":"two", "prop3":"false"}
          ],
          "content": [
            {"cont1":"val1", "cont2":"2", "cont3":"true"},
            {"cont1":"valA", "cont2":"two", "cont3":"false"}
          ],
          "merchandising": [
            {"merch1":"val1", "merch2":"2", "merch3":"true"},
            {"merch1":"valA", "merch2":"two", "merch3":"false"}
          ]
        }
        """

        fun getSegment(
            source: Map<String, String> = mapOf(
                "prop1" to "val1",
                "prop2" to "2",
                "prop3" to "true"
            )
        ): Segment {
            return Segment(source)
        }

        fun getSegmentsData(
            customerIds: HashMap<String, String?> = getCustomerIds().toHashMap(),
            data: SegmentationCategories = getSegmentations(),
            updateMillis: Long = System.currentTimeMillis()
        ): SegmentationData {
            return SegmentationData(
                customerIds = CustomerIds(customerIds).apply {
                    cookie = customerIds[CustomerIds.COOKIE]
                },
                segmentations = data,
                updatedAtMillis = updateMillis
            )
        }

        internal fun buildSegmentDataWithData(segmentData: Map<String, String>): SegmentationData {
            val segmentations = buildSingleSegmentWithData(segmentData)
            val segmentsData = getSegmentsData(data = segmentations)
            return segmentsData
        }

        internal fun buildSingleSegmentWithData(segmentData: Map<String, String>): SegmentationCategories {
            val segments = arrayListOf(getSegment(segmentData))
            val segmentations = getSegmentations(
                discovery = segments,
                content = arrayListOf(),
                merchandising = arrayListOf()
            )
            return segmentations
        }

        fun getSegmentations(
            discovery: ArrayList<Segment> = arrayListOf(
                Segment(mapOf(
                    "prop1" to "val1",
                    "prop2" to "2",
                    "prop3" to "true"
                )),
                Segment(mapOf(
                    "prop1" to "valA",
                    "prop2" to "two",
                    "prop3" to "false"
                ))
            ),
            content: ArrayList<Segment> = arrayListOf(
                Segment(mapOf(
                    "cont1" to "val1",
                    "cont2" to "2",
                    "cont3" to "true"
                )),
                Segment(mapOf(
                    "cont1" to "valA",
                    "cont2" to "two",
                    "cont3" to "false"
                ))
            ),
            merchandising: ArrayList<Segment> = arrayListOf(
                Segment(mapOf(
                    "merch1" to "val1",
                    "merch2" to "2",
                    "merch3" to "true"
                )),
                Segment(mapOf(
                    "merch1" to "valA",
                    "merch2" to "two",
                    "merch3" to "false"
                ))
            )
        ): SegmentationCategories {
            return SegmentationCategories(mapOf(
                "discovery" to discovery,
                "content" to content,
                "merchandising" to merchandising
            ))
        }

        fun getCustomerIds(
            customerIds: HashMap<String, String?> = hashMapOf(
                "cookie" to "mock-cookie",
                "registered" to "mock-registered"
            )
        ): CustomerIds {
            return CustomerIds(customerIds).apply {
                cookie = customerIds[CustomerIds.COOKIE]
            }
        }
    }

    @Test
    fun `should compare segments - empty segments`() {
        val segment1 = Segment(mapOf())
        val segment2 = Segment(mapOf())
        assertEquals(segment1, segment2)
    }

    @Test
    fun `should compare segments - same segments`() {
        val segment1 = Segment(mapOf("prop" to "val"))
        val segment2 = Segment(mapOf("prop" to "val"))
        assertEquals(segment1, segment2)
    }

    @Test
    fun `should compare segments - different by value segments`() {
        val segment1 = Segment(mapOf("prop" to "val"))
        val segment2 = Segment(mapOf("prop" to "another-val"))
        assertNotEquals(segment1, segment2)
    }

    @Test
    fun `should compare segments - different by key segments`() {
        val segment1 = Segment(mapOf("prop" to "val"))
        val segment2 = Segment(mapOf("another-prop" to "val"))
        assertNotEquals(segment1, segment2)
    }

    @Test
    fun `should compare segments - different by key and value segments`() {
        val segment1 = Segment(mapOf("prop" to "val"))
        val segment2 = Segment(mapOf("another-prop" to "another-val"))
        assertNotEquals(segment1, segment2)
    }

    @Test
    fun `should compare segment categories - empty`() {
        val category1 = SegmentationCategories()
        val category2 = SegmentationCategories()
        assertEquals(category1, category2)
    }

    @Test
    fun `should compare segment categories - same segments`() {
        val category1 = buildSingleSegmentWithData(mapOf("prop" to "val"))
        val category2 = buildSingleSegmentWithData(mapOf("prop" to "val"))
        assertEquals(category1, category2)
    }

    @Test
    fun `should compare segment categories - same data with different order`() {
        val category1 = buildSingleSegmentWithData(mapOf(
            "prop" to "val",
            "prop2" to "val2"
        ))
        val category2 = buildSingleSegmentWithData(mapOf(
            "prop2" to "val2",
            "prop" to "val"
        ))
        assertEquals(category1, category2)
    }

    @Test
    fun `should compare segment categories - different by value segments`() {
        val category1 = buildSingleSegmentWithData(mapOf("prop" to "val"))
        val category2 = buildSingleSegmentWithData(mapOf("prop" to "another-val"))
        assertNotEquals(category1, category2)
    }

    @Test
    fun `should compare segment categories - different by key segments`() {
        val category1 = buildSingleSegmentWithData(mapOf("prop" to "val"))
        val category2 = buildSingleSegmentWithData(mapOf("another-prop" to "val"))
        assertNotEquals(category1, category2)
    }

    @Test
    fun `should compare segment categories - different by key and value segments`() {
        val category1 = buildSingleSegmentWithData(mapOf("prop" to "val"))
        val category2 = buildSingleSegmentWithData(mapOf("another-prop" to "another-val"))
        assertNotEquals(category1, category2)
    }

    @Test
    fun `should compare segment categories - different by category`() {
        val category1 = SegmentationCategories(mapOf(
            "discovery" to arrayListOf(Segment(mapOf("prop" to "val")))
        ))
        val category2 = SegmentationCategories(mapOf(
            "content" to arrayListOf(Segment(mapOf("prop" to "val")))
        ))
        assertNotEquals(category1, category2)
    }

    @Test
    fun `should compare segment categories - same data with diff order`() {
        val category1 = SegmentationCategories(mapOf(
            "content" to arrayListOf(Segment(mapOf("prop" to "val"))),
            "discovery" to arrayListOf(Segment(mapOf("prop" to "val")))
        ))
        val category2 = SegmentationCategories(mapOf(
            "discovery" to arrayListOf(Segment(mapOf("prop" to "val"))),
            "content" to arrayListOf(Segment(mapOf("prop" to "val")))
        ))
        assertEquals(category1, category2)
    }

    @Test
    fun `should parse segment from json`() {
        val parsedSegment = Gson().fromJson(SEGMENT_JSON, object : TypeToken<Segment>() {})
        assertEquals(getSegment(), parsedSegment)
    }

    @Test
    fun `should parse segmentations from json`() {
        val parsedSegmentations = Gson().fromJson(SEGMENTATIONS_JSON, object : TypeToken<SegmentationCategories>() {})
        assertEquals(getSegmentations(), parsedSegmentations)
    }

    @Test
    fun `should compare SegmentsData that are equal by default customerIds and data`() {
        val now = System.currentTimeMillis()
        assertEquals(getSegmentsData(updateMillis = now), getSegmentsData(updateMillis = now))
    }

    @Test
    fun `should compare SegmentsData that are equal by custom customerIds and default data`() {
        val now = System.currentTimeMillis()
        val data1 = getSegmentsData(customerIds = hashMapOf(
            "cookie" to "cookie1",
            "registered" to "registered1"
        ), updateMillis = now)
        val data2 = getSegmentsData(customerIds = hashMapOf(
            "cookie" to "cookie1",
            "registered" to "registered1"
        ), updateMillis = now)
        assertEquals(data1, data2)
    }

    @Test
    fun `should compare SegmentsData that are equal by default customerIDs and custom data`() {
        val now = System.currentTimeMillis()
        val data1 = getSegmentsData(data = getSegmentations(
            discovery = arrayListOf(
                getSegment(mapOf(
                    "segment1" to "value1",
                    "segment2" to "2",
                    "segment3" to "true"
                ))
            )
        ), updateMillis = now)
        val data2 = getSegmentsData(data = getSegmentations(
            discovery = arrayListOf(
                getSegment(mapOf(
                    "segment1" to "value1",
                    "segment2" to "2",
                    "segment3" to "true"
                ))
            )
        ), updateMillis = now)
        assertEquals(data1, data2)
    }

    @Test
    fun `should compare SegmentsData that are equal by custom customerIDs and custom data`() {
        val now = System.currentTimeMillis()
        val data1 = getSegmentsData(
            customerIds = hashMapOf(
                "cookie" to "cookie1",
                "registered" to "registered1"
            ),
            data = getSegmentations(
                discovery = arrayListOf(
                    getSegment(mapOf(
                        "segment1" to "value1",
                        "segment2" to "2",
                        "segment3" to "true"
                    ))
                )
            ),
            updateMillis = now
        )
        val data2 = getSegmentsData(
            customerIds = hashMapOf(
                "cookie" to "cookie1",
                "registered" to "registered1"
            ),
            data = getSegmentations(
                discovery = arrayListOf(
                    getSegment(mapOf(
                        "segment1" to "value1",
                        "segment2" to "2",
                        "segment3" to "true"
                    ))
                )
            ),
            updateMillis = now
        )
        assertEquals(data1, data2)
    }

    @Test
    fun `should compare SegmentsData that are non-equal by custom customerIds and default data`() {
        val now = System.currentTimeMillis()
        val data1 = getSegmentsData(customerIds = hashMapOf(
            "cookie" to "cookie1",
            "registered" to "registered1"
        ), updateMillis = now)
        val data2 = getSegmentsData(customerIds = hashMapOf(
            "cookie" to "cookie2",
            "registered" to "registered2"
        ), updateMillis = now)
        assertNotEquals(data1, data2)
    }

    @Test
    fun `should compare SegmentsData that are non-equal by default customerIDs and custom data`() {
        val now = System.currentTimeMillis()
        val data1 = getSegmentsData(data = getSegmentations(
            discovery = arrayListOf(
                getSegment(mapOf(
                    "segment1" to "value1",
                    "segment2" to "2",
                    "segment3" to "true"
                ))
            )
        ), updateMillis = now)
        val data2 = getSegmentsData(data = getSegmentations(
            discovery = arrayListOf(
                getSegment(mapOf(
                    "segment1" to "value2",
                    "segment2" to "3",
                    "segment3" to "false"
                ))
            )
        ), updateMillis = now)
        assertNotEquals(data1, data2)
    }

    @Test
    fun `should compare SegmentsData that are non-equal by custom customerIDs and custom data`() {
        val now = System.currentTimeMillis()
        val data1 = getSegmentsData(
            customerIds = hashMapOf(
                "cookie" to "cookie1",
                "registered" to "registered1"
            ),
            data = getSegmentations(
                discovery = arrayListOf(
                    getSegment(mapOf(
                        "segment1" to "value1",
                        "segment2" to "2",
                        "segment3" to "true"
                    ))
                )
            ),
            updateMillis = now
        )
        val data2 = getSegmentsData(
            customerIds = hashMapOf(
                "cookie" to "cookie2",
                "registered" to "registered2"
            ),
            data = getSegmentations(
                discovery = arrayListOf(
                    getSegment(mapOf(
                        "segment1" to "value2",
                        "segment2" to "3",
                        "segment3" to "false"
                    ))
                )
            ),
            updateMillis = now
        )
        assertNotEquals(data1, data2)
    }
}
