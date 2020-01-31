package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.models.eventfilter.EventFilterOperand
import com.exponea.sdk.models.eventfilter.PropertyAttribute
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

internal class EventFilterBooleanOperatorsTest {
    companion object {
        val testEvent = EventFilterEvent(
            "test",
            hashMapOf("string" to "something", "number" to 1234, "true" to true, "false" to false, "null" to null),
            12345.0
        )
        val trueOperand = arrayListOf(EventFilterOperand("true"))
        val falseOperand = arrayListOf(EventFilterOperand("false"))
    }

    @Test
    fun `test 'is'`() {
        assertTrue(IsOperator().passes(testEvent, PropertyAttribute("true"), trueOperand))
        assertTrue(IsOperator().passes(testEvent, PropertyAttribute("false"), falseOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("true"), falseOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("false"), trueOperand))

        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("boolean"), trueOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("string"), trueOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("number"), trueOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("null"), trueOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("non_existing_prop"), trueOperand))

        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("boolean"), falseOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("string"), falseOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("number"), falseOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("null"), falseOperand))
        assertFalse(IsOperator().passes(testEvent, PropertyAttribute("non_existing_prop"), falseOperand))
    }
}
