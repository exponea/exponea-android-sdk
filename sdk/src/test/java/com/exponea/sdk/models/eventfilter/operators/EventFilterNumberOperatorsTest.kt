package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterAttribute
import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.models.eventfilter.EventFilterOperand
import com.exponea.sdk.models.eventfilter.EventFilterOperator
import com.exponea.sdk.models.eventfilter.PropertyAttribute
import com.exponea.sdk.models.eventfilter.TimestampAttribute
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

internal class EventFilterNumberOperatorsTest {
    companion object {
        val testEvent = EventFilterEvent(
            "test",
            hashMapOf(
                "string" to "something",
                "integer" to 123,
                "zero" to 0,
                "pi" to 3.14159,
                "boolean" to true,
                "null" to null
            ),
            12345.0
        )
    }

    private fun passes(
        operator: EventFilterOperator,
        attribute: EventFilterAttribute,
        operandValues: List<String>
    ): Boolean {
        return operator.passes(testEvent, attribute, operandValues.map { EventFilterOperand(it) })
    }

    @Test
    fun `test 'equal to'`() {
        assertFalse(passes(EqualToOperator(), PropertyAttribute("string"), arrayListOf("0")))
        assertFalse(passes(EqualToOperator(), PropertyAttribute("integer"), arrayListOf("0")))
        assertTrue(passes(EqualToOperator(), PropertyAttribute("zero"), arrayListOf("0")))
        assertFalse(passes(EqualToOperator(), PropertyAttribute("pi"), arrayListOf("0")))
        assertFalse(passes(EqualToOperator(), PropertyAttribute("boolean"), arrayListOf("0")))
        assertFalse(passes(EqualToOperator(), PropertyAttribute("null"), arrayListOf("0")))
        assertFalse(passes(EqualToOperator(), PropertyAttribute("non-existing"), arrayListOf("0")))

        assertTrue(passes(EqualToOperator(), PropertyAttribute("pi"), arrayListOf("3.14159")))
        assertFalse(passes(EqualToOperator(), TimestampAttribute(), arrayListOf("3.14159")))
        assertTrue(passes(EqualToOperator(), TimestampAttribute(), arrayListOf("12345")))
        assertTrue(passes(EqualToOperator(), PropertyAttribute("integer"), arrayListOf("123")))
    }

    @Test
    fun `test 'in between'`() {
        assertFalse(passes(InBetweenOperator(), PropertyAttribute("string"), arrayListOf("-1", "1")))
        assertFalse(passes(InBetweenOperator(), PropertyAttribute("integer"), arrayListOf("-1", "1")))
        assertTrue(passes(InBetweenOperator(), PropertyAttribute("zero"), arrayListOf("-1", "1")))
        assertFalse(passes(InBetweenOperator(), PropertyAttribute("pi"), arrayListOf("-1", "1")))
        assertFalse(passes(InBetweenOperator(), PropertyAttribute("boolean"), arrayListOf("-1", "1")))
        assertFalse(passes(InBetweenOperator(), PropertyAttribute("null"), arrayListOf("-1", "1")))
        assertFalse(passes(InBetweenOperator(), PropertyAttribute("non-existing"), arrayListOf("0", "1")))

        assertTrue(passes(InBetweenOperator(), PropertyAttribute("pi"), arrayListOf("3", "3.2")))
        assertTrue(passes(InBetweenOperator(), PropertyAttribute("integer"), arrayListOf("123", "123")))
        assertTrue(passes(InBetweenOperator(), TimestampAttribute(), arrayListOf("12345", "12345")))
    }

    @Test
    fun `test 'not between'`() {
        assertFalse(passes(NotBetweenOperator(), PropertyAttribute("string"), arrayListOf("-1", "1")))
        assertTrue(passes(NotBetweenOperator(), PropertyAttribute("integer"), arrayListOf("-1", "1")))
        assertFalse(passes(NotBetweenOperator(), PropertyAttribute("zero"), arrayListOf("-1", "1")))
        assertTrue(passes(NotBetweenOperator(), PropertyAttribute("pi"), arrayListOf("-1", "1")))
        assertFalse(passes(NotBetweenOperator(), PropertyAttribute("boolean"), arrayListOf("-1", "1")))
        assertFalse(passes(NotBetweenOperator(), PropertyAttribute("null"), arrayListOf("-1", "1")))
        assertFalse(passes(NotBetweenOperator(), PropertyAttribute("non-existing"), arrayListOf("0", "1")))

        assertFalse(passes(NotBetweenOperator(), PropertyAttribute("pi"), arrayListOf("3", "3.2")))
        assertFalse(passes(NotBetweenOperator(), PropertyAttribute("integer"), arrayListOf("123", "123")))
        assertFalse(passes(NotBetweenOperator(), TimestampAttribute(), arrayListOf("12345", "12345")))
    }

    @Test
    fun `test 'less than'`() {
        assertFalse(passes(LessThanOperator(), PropertyAttribute("string"), arrayListOf("0")))
        assertFalse(passes(LessThanOperator(), PropertyAttribute("integer"), arrayListOf("0")))
        assertFalse(passes(LessThanOperator(), PropertyAttribute("zero"), arrayListOf("0")))
        assertFalse(passes(LessThanOperator(), PropertyAttribute("pi"), arrayListOf("0")))
        assertFalse(passes(LessThanOperator(), PropertyAttribute("boolean"), arrayListOf("0")))
        assertFalse(passes(LessThanOperator(), PropertyAttribute("null"), arrayListOf("0")))
        assertFalse(passes(LessThanOperator(), PropertyAttribute("non-existing"), arrayListOf("0")))

        assertTrue(passes(LessThanOperator(), PropertyAttribute("pi"), arrayListOf("3.1416")))
        assertFalse(passes(LessThanOperator(), TimestampAttribute(), arrayListOf("3.14159")))
        assertTrue(passes(LessThanOperator(), TimestampAttribute(), arrayListOf("12346")))
        assertTrue(passes(LessThanOperator(), PropertyAttribute("integer"), arrayListOf("123.1")))
    }

    @Test
    fun `test 'greater than'`() {
        assertFalse(passes(GreaterThanOperator(), PropertyAttribute("string"), arrayListOf("0")))
        assertTrue(passes(GreaterThanOperator(), PropertyAttribute("integer"), arrayListOf("0")))
        assertFalse(passes(GreaterThanOperator(), PropertyAttribute("zero"), arrayListOf("0")))
        assertTrue(passes(GreaterThanOperator(), PropertyAttribute("pi"), arrayListOf("0")))
        assertFalse(passes(GreaterThanOperator(), PropertyAttribute("boolean"), arrayListOf("0")))
        assertFalse(passes(GreaterThanOperator(), PropertyAttribute("null"), arrayListOf("0")))
        assertFalse(passes(GreaterThanOperator(), PropertyAttribute("non-existing"), arrayListOf("0")))

        assertTrue(passes(GreaterThanOperator(), PropertyAttribute("pi"), arrayListOf("3.14")))
        assertTrue(passes(GreaterThanOperator(), TimestampAttribute(), arrayListOf("3.14159")))
        assertTrue(passes(GreaterThanOperator(), TimestampAttribute(), arrayListOf("12344")))
        assertTrue(passes(GreaterThanOperator(), PropertyAttribute("integer"), arrayListOf("122.99")))
    }
}
