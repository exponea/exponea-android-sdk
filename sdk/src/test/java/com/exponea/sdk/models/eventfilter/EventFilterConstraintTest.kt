package com.exponea.sdk.models.eventfilter

import com.google.gson.JsonParseException
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(Enclosed::class)
internal class EventFilterConstraintTest {
    internal class EventFiltercontraintSerializationTest {
        @Test(expected = JsonParseException::class)
        fun `should thrown on unknown constraint type`() {
            EventFilter.gson.fromJson(
                """{"type":"mock_type","operator":"is set","operands":[]}""",
                EventFilterConstraint::class.java
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    internal class EventFilterConstraintCaseTest(
        constraintType: String,
        operatorName: String,
        val constraint: EventFilterConstraint,
        val serializedConstraint: String
    ) {
        companion object {
            data class TestCase(
                val constraint: EventFilterConstraint,
                val serializedConstraint: String
            )

            val testCases = arrayListOf(
                // string
                TestCase(
                    StringConstraint.isSet,
                    """{"type":"string","operator":"is set","operands":[]}"""
                ),
                TestCase(
                    StringConstraint.isNotSet,
                    """{"type":"string","operator":"is not set","operands":[]}"""
                ),
                TestCase(
                    StringConstraint.hasValue,
                    """{"type":"string","operator":"has value","operands":[]}"""
                ),
                TestCase(
                    StringConstraint.hasNoValue,
                    """{"type":"string","operator":"has no value","operands":[]}"""
                ),
                TestCase(
                    StringConstraint.equals("other"),
                    """{"type":"string","operator":"equals","operands":[{"type":"constant","value":"other"}]}"""
                ),
                TestCase(
                    StringConstraint.doesNotEqual("other"),
                    """{"type":"string","operator":"does not equal","operands":[{"type":"constant","value":"other"}]}"""
                ),
                TestCase(
                    StringConstraint.isIn(arrayListOf("other")),
                    """{"type":"string","operator":"in","operands":[{"type":"constant","value":"other"}]}"""
                ),
                TestCase(
                    StringConstraint.notIn(arrayListOf("other")),
                    """{"type":"string","operator":"not in","operands":[{"type":"constant","value":"other"}]}"""
                ),
                TestCase(
                    StringConstraint.contains("other"),
                    """{"type":"string","operator":"contains","operands":[{"type":"constant","value":"other"}]}"""
                ),
                TestCase(
                    StringConstraint.doesNotContain("other"),
                    """{"type":"string","operator":"does not contain","operands":[{"type":"constant","value":"other"}]}""" // ktlint-disable max-line-length
                ),
                TestCase(
                    StringConstraint.startsWith("other"),
                    """{"type":"string","operator":"starts with","operands":[{"type":"constant","value":"other"}]}"""
                ),
                TestCase(
                    StringConstraint.endsWith("other"),
                    """{"type":"string","operator":"ends with","operands":[{"type":"constant","value":"other"}]}"""
                ),
                TestCase(
                    StringConstraint.regex("other"),
                    """{"type":"string","operator":"regex","operands":[{"type":"constant","value":"other"}]}"""
                ),
                // number
                TestCase(
                    NumberConstraint.isSet,
                    """{"type":"number","operator":"is set","operands":[]}"""
                ),
                TestCase(
                    NumberConstraint.isNotSet,
                    """{"type":"number","operator":"is not set","operands":[]}"""
                ),
                TestCase(
                    NumberConstraint.hasValue,
                    """{"type":"number","operator":"has value","operands":[]}"""
                ),
                TestCase(
                    NumberConstraint.hasNoValue,
                    """{"type":"number","operator":"has no value","operands":[]}"""
                ),
                TestCase(
                    NumberConstraint.equalTo(123),
                    """{"type":"number","operator":"equal to","operands":[{"type":"constant","value":"123"}]}"""
                ),
                TestCase(
                    NumberConstraint.lessThan(123.0),
                    """{"type":"number","operator":"less than","operands":[{"type":"constant","value":"123.0"}]}"""
                ),
                TestCase(
                    NumberConstraint.greaterThan(123.0),
                    """{"type":"number","operator":"greater than","operands":[{"type":"constant","value":"123.0"}]}"""
                ),
                TestCase(
                    NumberConstraint.inBetween(1, 2),
                    """{"type":"number","operator":"in between","operands":[{"type":"constant","value":"1"},{"type":"constant","value":"2"}]}""" // ktlint-disable max-line-length
                ),
                TestCase(
                    NumberConstraint.notBetween(1.123, 2.345),
                    """{"type":"number","operator":"not between","operands":[{"type":"constant","value":"1.123"},{"type":"constant","value":"2.345"}]}""" // ktlint-disable max-line-length
                ),
                // boolean
                TestCase(
                    BooleanConstraint.isSet,
                    """{"type":"boolean","operator":"is set","value":"true"}"""
                ),
                TestCase(
                    BooleanConstraint.isNotSet,
                    """{"type":"boolean","operator":"is not set","value":"true"}"""
                ),
                TestCase(
                    BooleanConstraint.hasValue,
                    """{"type":"boolean","operator":"has value","value":"true"}"""
                ),
                TestCase(
                    BooleanConstraint.hasNoValue,
                    """{"type":"boolean","operator":"has no value","value":"true"}"""
                ),
                TestCase(
                    BooleanConstraint.itIs(false),
                    """{"type":"boolean","operator":"is","value":"false"}"""
                )
            )

            @JvmStatic
            @ParameterizedRobolectricTestRunner.Parameters(
                name = "Serializing \"{0}\" constraint with \"{1}\" operator"
            )
            fun data(): List<Array<out Any?>> {
                return testCases.map {
                    arrayOf(
                        it.constraint.type,
                        it.constraint.operator.name,
                        it.constraint,
                        it.serializedConstraint
                    )
                }
            }
        }

        @Test
        fun `should serialize`() {
            assertEquals(serializedConstraint, EventFilter.gson.toJson(constraint))
        }

        @Test
        fun `should deserialize`() {
            assertEquals(constraint, EventFilter.gson.fromJson(serializedConstraint, EventFilterConstraint::class.java))
        }
    }
}
