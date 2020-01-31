package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.models.eventfilter.PropertyAttribute
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class EventFilterGenericOperatorsTest {
    companion object {
        val testEvent = EventFilterEvent(
            "test",
            hashMapOf("string" to "something", "number" to 1234, "boolean" to true, "null" to null),
            12345.0
        )
    }

    @Test
    fun `test 'is set'`() {
        assertTrue(IsSetOperator().passes(testEvent, PropertyAttribute("string"), arrayListOf()))
        assertFalse(IsSetOperator().passes(testEvent, PropertyAttribute("non_existing_prop"), arrayListOf()))
    }

    @Test
    fun `test 'is not set'`() {
        assertFalse(IsNotSetOperator().passes(testEvent, PropertyAttribute("string"), arrayListOf()))
        assertTrue(IsNotSetOperator().passes(testEvent, PropertyAttribute("non_existing_prop"), arrayListOf()))
    }

    @Test
    fun `test 'has value'`() {
        assertFalse(HasValueOperator().passes(testEvent, PropertyAttribute("non_existing_prop"), arrayListOf()))
        assertFalse(HasValueOperator().passes(testEvent, PropertyAttribute("null"), arrayListOf()))
        assertTrue(HasValueOperator().passes(testEvent, PropertyAttribute("string"), arrayListOf()))
    }

    @Test
    fun `test 'has no value'`() {
        assertFalse(HasNoValueOperator().passes(testEvent, PropertyAttribute("non_existing_prop"), arrayListOf()))
        assertTrue(HasNoValueOperator().passes(testEvent, PropertyAttribute("null"), arrayListOf()))
        assertFalse(HasNoValueOperator().passes(testEvent, PropertyAttribute("string"), arrayListOf()))
    }
}
