package com.exponea.sdk.models.eventfilter

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

internal class EventFilterTest {
    @Test
    fun `should pass correct events`() {
        val eventFilter = EventFilter(
            eventType = "session_start",
            filter = arrayListOf(
                EventPropertyFilter.timestamp(NumberConstraint.greaterThan(123.0)),
                EventPropertyFilter.property("property", StringConstraint.startsWith("val")),
                EventPropertyFilter.property("other_property", BooleanConstraint.isSet)
            )
        )
        assertTrue(
            eventFilter.passes(
                EventFilterEvent(
                    "session_start",
                    hashMapOf("property" to "value", "other_property" to null),
                    1234567890.0
                )
            )
        )
        assertFalse(
            eventFilter.passes(
                EventFilterEvent(
                    "session_end",
                    hashMapOf("property" to "value", "other_property" to null),
                    1234567890.0
                )
            )
        )
        assertFalse(
            eventFilter.passes(
                EventFilterEvent(
                    "session_start",
                    hashMapOf("property" to "other value", "other_property" to null),
                    1234567890.0
                )
            )
        )
    }

    @Test(expected = EventFilterOperatorException::class)
    fun `should throw on invalid operands`() {
        val payload = """
        {
            "event_type":"banner",
            "filter":[
                {
                    "attribute":{
                        "type":"property",
                        "property":"os_version"
                    },
                    "constraint":{
                        "operator":"equals",
                        "operands":[],
                        "type":"string"
                    }
                }
            ]
        }
        """
        val filter = EventFilter.deserialize(payload)
        filter.passes(EventFilterEvent("banner", hashMapOf(), 1234567890.0))
    }

    @Test
    fun `should serialize and deserialize example payload`() {
        val eventFilter = EventFilter(
            eventType = "session_start",
            filter = arrayListOf(
                EventPropertyFilter.timestamp(NumberConstraint.greaterThan(123.0)),
                EventPropertyFilter.property("property", StringConstraint.startsWith("val")),
                EventPropertyFilter.property("other_property", BooleanConstraint.isSet)
            )
        )
        assertEquals(eventFilter, EventFilter.deserialize(eventFilter.serialize()))
    }

    @Test
    fun `should deserialize payload from server`() {
        val data = """
            {
                "event_type":"banner",
                "filter":[
                    {
                        "attribute":{
                            "type":"property",
                            "property":"os_version"
                        },
                        "constraint":{
                            "operator":"equals",
                            "operands":[
                                {
                                    "type":"constant",
                                    "value":"10"
                                }
                            ],
                            "type":"string"
                        }
                    },
                    {
                        "attribute":{
                            "type":"property",
                            "property":"platform"
                        },
                        "constraint":{
                            "operator":"is set",
                            "operands":[

                            ],
                            "type":"boolean",
                            "value":"true"
                        }
                    },
                    {
                        "attribute":{
                            "type":"property",
                            "property":"type"
                        },
                        "constraint":{
                            "operator":"greater than",
                            "operands":[
                                {
                                    "type":"constant",
                                    "value":"123"
                                }
                            ],
                            "type":"number"
                        }
                    },
                    {
                        "attribute":{
                            "type":"timestamp"
                        },
                        "constraint":{
                            "operator":"less than",
                            "operands":[
                                {
                                    "type":"constant",
                                    "value":"456"
                                }
                            ],
                            "type":"number"
                        }
                    }
                ]
            }
        """
        assertEquals(
            EventFilter(
                eventType = "banner",
                filter = arrayListOf(
                    EventPropertyFilter.property("os_version", StringConstraint.equals("10")),
                    EventPropertyFilter.property("platform", BooleanConstraint.isSet),
                    EventPropertyFilter.property("type", NumberConstraint.greaterThan(123)),
                    EventPropertyFilter.timestamp(NumberConstraint.lessThan(456))
                )
            ),
            EventFilter.deserialize(data)
        )
    }
}
