package com.exponea.sdk.models.eventfilter

import com.google.gson.JsonParseException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

internal class EventFilterAttributeTest {
    @Test
    fun `should parse property attribute`() {
        val attribute = EventFilter.gson.fromJson(
            """{"type":"property","property":"os_version"}""",
            EventFilterAttribute::class.java
        )
        assertTrue(attribute is PropertyAttribute)
        assertEquals("os_version", attribute.property)
    }

    @Test
    fun `should parse timestamp attribute`() {
        val attribute = EventFilter.gson.fromJson(
            """{"type":"timestamp"}""",
            EventFilterAttribute::class.java
        )
        assertTrue(attribute is TimestampAttribute)
    }

    @Test(expected = JsonParseException::class)
    fun `should thrown on unknown attribute type`() {
        EventFilter.gson.fromJson(
            """{"type":"mock_type"}""",
            EventFilterAttribute::class.java
        )
    }

    @Test
    fun `should get value of property attribute`() {
        val event = EventFilterEvent("value", hashMapOf("null" to null, "value" to "value"), null)
        assertTrue(PropertyAttribute("null").isSet(event))
        assertTrue(PropertyAttribute("value").isSet(event))
        assertFalse(PropertyAttribute("non-existing").isSet(event))
        assertEquals(null, PropertyAttribute("null").getValue(event))
        assertEquals("value", PropertyAttribute("value").getValue(event))
        assertEquals(null, PropertyAttribute("non-existing").getValue(event))
    }

    @Test
    fun `should get value of timestamp attribute`() {
        assertFalse(TimestampAttribute().isSet(EventFilterEvent("value", hashMapOf(), null)))
        assertTrue(TimestampAttribute().isSet(EventFilterEvent("value", hashMapOf(), 12345.0)))
        assertEquals(null, TimestampAttribute().getValue(EventFilterEvent("value", hashMapOf(), null)))
        assertEquals("12345.0", TimestampAttribute().getValue(EventFilterEvent("value", hashMapOf(), 12345.0)))
    }
}
