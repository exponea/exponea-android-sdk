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

internal class EventFilterStringOperatorsTest {
    companion object {
        val testEvent = EventFilterEvent(
            "test",
            hashMapOf("string" to "something", "number" to 1234, "true" to true, "false" to false, "null" to null),
            12345.0
        )

        private fun passes(
            operator: EventFilterOperator,
            attribute: EventFilterAttribute,
            operandValues: List<String>
        ): Boolean {
            return operator.passes(testEvent, attribute, operandValues.map { EventFilterOperand(it) })
        }
    }

    @Test
    fun `test 'equals'`() {
        assertTrue(passes(EqualsOperator(), PropertyAttribute("string"), arrayListOf("something")))
        assertFalse(passes(EqualsOperator(), PropertyAttribute("number"), arrayListOf("something")))
        assertFalse(passes(EqualsOperator(), PropertyAttribute("true"), arrayListOf("something")))
        assertFalse(passes(EqualsOperator(), PropertyAttribute("false"), arrayListOf("something")))
        assertFalse(passes(EqualsOperator(), PropertyAttribute("null"), arrayListOf("something")))
        assertFalse(passes(EqualsOperator(), PropertyAttribute("non-existing"), arrayListOf("something")))

        assertTrue(passes(EqualsOperator(), PropertyAttribute("number"), arrayListOf("1234")))
        assertTrue(passes(EqualsOperator(), PropertyAttribute("true"), arrayListOf("true")))
        assertTrue(passes(EqualsOperator(), PropertyAttribute("false"), arrayListOf("false")))
        assertFalse(passes(EqualsOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(EqualsOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertTrue(passes(EqualsOperator(), TimestampAttribute(), arrayListOf("12345.0")))
    }

    @Test
    fun `test 'does not equal'`() {
        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("string"), arrayListOf("something")))
        assertTrue(passes(DoesNotEqualOperator(), PropertyAttribute("number"), arrayListOf("something")))
        assertTrue(passes(DoesNotEqualOperator(), PropertyAttribute("true"), arrayListOf("something")))
        assertTrue(passes(DoesNotEqualOperator(), PropertyAttribute("false"), arrayListOf("something")))
        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("null"), arrayListOf("something")))
        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("non-existing"), arrayListOf("something")))

        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("number"), arrayListOf("1234")))
        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("true"), arrayListOf("true")))
        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("false"), arrayListOf("false")))
        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(DoesNotEqualOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertFalse(passes(DoesNotEqualOperator(), TimestampAttribute(), arrayListOf("12345.0")))
    }

    @Test
    fun `test 'in'`() {
        assertTrue(passes(InOperator(), PropertyAttribute("string"), arrayListOf("something", "false")))
        assertFalse(passes(InOperator(), PropertyAttribute("number"), arrayListOf("something", "false")))
        assertFalse(passes(InOperator(), PropertyAttribute("true"), arrayListOf("something", "false")))
        assertTrue(passes(InOperator(), PropertyAttribute("false"), arrayListOf("something", "false")))
        assertFalse(passes(InOperator(), PropertyAttribute("null"), arrayListOf("something", "false")))
        assertFalse(passes(InOperator(), PropertyAttribute("non-existing"), arrayListOf("something")))

        assertTrue(passes(InOperator(), PropertyAttribute("number"), arrayListOf("1", "1234")))
        assertTrue(passes(InOperator(), PropertyAttribute("true"), arrayListOf("true", "false")))
        assertTrue(passes(InOperator(), PropertyAttribute("false"), arrayListOf("true", "false")))
        assertFalse(passes(InOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(InOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertTrue(passes(InOperator(), TimestampAttribute(), arrayListOf("12345.0")))
    }

    @Test
    fun `test 'not in'`() {
        assertFalse(passes(NotInOperator(), PropertyAttribute("string"), arrayListOf("something", "false")))
        assertTrue(passes(NotInOperator(), PropertyAttribute("number"), arrayListOf("something", "false")))
        assertTrue(passes(NotInOperator(), PropertyAttribute("true"), arrayListOf("something", "false")))
        assertFalse(passes(NotInOperator(), PropertyAttribute("false"), arrayListOf("something", "false")))
        assertFalse(passes(NotInOperator(), PropertyAttribute("null"), arrayListOf("something", "false")))
        assertFalse(passes(NotInOperator(), PropertyAttribute("non-existing"), arrayListOf("something")))

        assertFalse(passes(NotInOperator(), PropertyAttribute("number"), arrayListOf("1", "1234")))
        assertFalse(passes(NotInOperator(), PropertyAttribute("true"), arrayListOf("true", "false")))
        assertFalse(passes(NotInOperator(), PropertyAttribute("false"), arrayListOf("true", "false")))
        assertFalse(passes(NotInOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(NotInOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertTrue(passes(NotInOperator(), TimestampAttribute(), arrayListOf("12345")))
    }

    @Test
    fun `test 'contains'`() {
        assertTrue(passes(ContainsOperator(), PropertyAttribute("string"), arrayListOf("t")))
        assertFalse(passes(ContainsOperator(), PropertyAttribute("number"), arrayListOf("t")))
        assertTrue(passes(ContainsOperator(), PropertyAttribute("true"), arrayListOf("t")))
        assertFalse(passes(ContainsOperator(), PropertyAttribute("false"), arrayListOf("t")))
        assertFalse(passes(ContainsOperator(), PropertyAttribute("null"), arrayListOf("t")))
        assertFalse(passes(ContainsOperator(), PropertyAttribute("non-existing"), arrayListOf("t")))

        assertTrue(passes(ContainsOperator(), PropertyAttribute("number"), arrayListOf("2")))
        assertTrue(passes(ContainsOperator(), PropertyAttribute("true"), arrayListOf("ru")))
        assertTrue(passes(ContainsOperator(), PropertyAttribute("false"), arrayListOf("false")))
        assertFalse(passes(ContainsOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(ContainsOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertTrue(passes(ContainsOperator(), TimestampAttribute(), arrayListOf("12345")))
    }

    @Test
    fun `test 'does not contain'`() {
        assertFalse(passes(DoesNotContainOperator(), PropertyAttribute("string"), arrayListOf("t")))
        assertTrue(passes(DoesNotContainOperator(), PropertyAttribute("number"), arrayListOf("t")))
        assertFalse(passes(DoesNotContainOperator(), PropertyAttribute("true"), arrayListOf("t")))
        assertTrue(passes(DoesNotContainOperator(), PropertyAttribute("false"), arrayListOf("t")))
        assertFalse(passes(DoesNotContainOperator(), PropertyAttribute("null"), arrayListOf("t")))
        assertFalse(passes(DoesNotContainOperator(), PropertyAttribute("non-existing"), arrayListOf("t")))

        assertTrue(passes(DoesNotContainOperator(), PropertyAttribute("number"), arrayListOf("122")))
        assertTrue(passes(DoesNotContainOperator(), PropertyAttribute("true"), arrayListOf("truedat")))
        assertFalse(passes(DoesNotContainOperator(), PropertyAttribute("false"), arrayListOf("false")))
        assertFalse(passes(DoesNotContainOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(DoesNotContainOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertFalse(passes(DoesNotContainOperator(), TimestampAttribute(), arrayListOf("12345")))
    }

    @Test
    fun `test 'starts with'`() {
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("string"), arrayListOf("t")))
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("number"), arrayListOf("t")))
        assertTrue(passes(StartsWithOperator(), PropertyAttribute("true"), arrayListOf("t")))
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("false"), arrayListOf("t")))
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("null"), arrayListOf("t")))
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("non-existing"), arrayListOf("t")))

        assertTrue(passes(StartsWithOperator(), PropertyAttribute("number"), arrayListOf("12")))
        assertTrue(passes(StartsWithOperator(), PropertyAttribute("true"), arrayListOf("tru")))
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("false"), arrayListOf("alse")))
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(StartsWithOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertTrue(passes(StartsWithOperator(), TimestampAttribute(), arrayListOf("12345")))
    }

    @Test
    fun `test 'ends with'`() {
        assertFalse(passes(EndsWithOperator(), PropertyAttribute("string"), arrayListOf("e")))
        assertFalse(passes(EndsWithOperator(), PropertyAttribute("number"), arrayListOf("e")))
        assertTrue(passes(EndsWithOperator(), PropertyAttribute("true"), arrayListOf("e")))
        assertTrue(passes(EndsWithOperator(), PropertyAttribute("false"), arrayListOf("e")))
        assertFalse(passes(EndsWithOperator(), PropertyAttribute("null"), arrayListOf("e")))
        assertFalse(passes(EndsWithOperator(), PropertyAttribute("non-existing"), arrayListOf("e")))

        assertFalse(passes(EndsWithOperator(), PropertyAttribute("number"), arrayListOf("12")))
        assertFalse(passes(EndsWithOperator(), PropertyAttribute("true"), arrayListOf("tru")))
        assertTrue(passes(EndsWithOperator(), PropertyAttribute("false"), arrayListOf("alse")))
        assertFalse(passes(EndsWithOperator(), PropertyAttribute("null"), arrayListOf("null")))
        assertFalse(passes(EndsWithOperator(), PropertyAttribute("non-existing"), arrayListOf("null")))

        assertTrue(passes(EndsWithOperator(), TimestampAttribute(), arrayListOf("45.0")))
    }

    @Test
    fun `test 'regex'`() {
        assertTrue(passes(RegexOperator(), PropertyAttribute("string"), arrayListOf("^(some)*(123)*(thing)*")))
        assertTrue(passes(RegexOperator(), PropertyAttribute("number"), arrayListOf("^(some)*(123)*(thing)*")))
        assertFalse(passes(RegexOperator(), PropertyAttribute("true"), arrayListOf("^(some)*(123)*(thing)*$")))
        assertFalse(passes(RegexOperator(), PropertyAttribute("false"), arrayListOf("^(some)*(123)*(thing)*$")))
        assertFalse(passes(RegexOperator(), PropertyAttribute("null"), arrayListOf("^(some)*(123)*(thing)*")))
        assertFalse(passes(RegexOperator(), PropertyAttribute("non-existing"), arrayListOf("^(some)*(123)*(thing)*")))

        assertTrue(passes(RegexOperator(), PropertyAttribute("string"), arrayListOf("")))
        assertFalse(passes(RegexOperator(), PropertyAttribute("string"), arrayListOf("test")))
        assertTrue(passes(RegexOperator(), PropertyAttribute("string"), arrayListOf("^someX*thing$")))
        assertFalse(passes(RegexOperator(), PropertyAttribute("string"), arrayListOf("^someX+thing$")))
    }
}
