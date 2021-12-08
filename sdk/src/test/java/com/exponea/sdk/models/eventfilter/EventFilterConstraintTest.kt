package com.exponea.sdk.models.eventfilter

import com.exponea.sdk.util.ExponeaGson
import com.google.gson.JsonParseException
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(Enclosed::class)
internal class EventFilterConstraintTest {
    internal class EventFiltercontraintSerializationTest {
        @Test(expected = JsonParseException::class)
        fun `should thrown on unknown constraint type`() {
            ExponeaGson.instance.fromJson(
                """{"type":"mock_type","operator":"is set","operands":[]}""",
                EventFilterConstraint::class.java
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    internal class EventFilterConstraintCaseTest(
        @Suppress("UNUSED_PARAMETER")
        constraintType: String,
        @Suppress("UNUSED_PARAMETER")
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
                    """{"operator":"is set","operands":[],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.isNotSet,
                    """{"operator":"is not set","operands":[],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.hasValue,
                    """{"operator":"has value","operands":[],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.hasNoValue,
                    """{"operator":"has no value","operands":[],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.equals("other"),
                    """{"operator":"equals","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.doesNotEqual("other"),
                    """{"operator":"does not equal","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.isIn(arrayListOf("other")),
                    """{"operator":"in","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.notIn(arrayListOf("other")),
                    """{"operator":"not in","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.contains("other"),
                    """{"operator":"contains","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.doesNotContain("other"),
                    """{"operator":"does not contain","operands":[{"value":"other","type":"constant"}],"type":"string"}""" // ktlint-disable max-line-length
                ),
                TestCase(
                    StringConstraint.startsWith("other"),
                    """{"operator":"starts with","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.endsWith("other"),
                    """{"operator":"ends with","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                TestCase(
                    StringConstraint.regex("other"),
                    """{"operator":"regex","operands":[{"value":"other","type":"constant"}],"type":"string"}"""
                ),
                // number
                TestCase(
                    NumberConstraint.isSet,
                    """{"operator":"is set","operands":[],"type":"number"}"""
                ),
                TestCase(
                    NumberConstraint.isNotSet,
                    """{"operator":"is not set","operands":[],"type":"number"}"""
                ),
                TestCase(
                    NumberConstraint.hasValue,
                    """{"operator":"has value","operands":[],"type":"number"}"""
                ),
                TestCase(
                    NumberConstraint.hasNoValue,
                    """{"operator":"has no value","operands":[],"type":"number"}"""
                ),
                TestCase(
                    NumberConstraint.equalTo(123),
                    """{"operator":"equal to","operands":[{"value":"123","type":"constant"}],"type":"number"}"""
                ),
                TestCase(
                    NumberConstraint.lessThan(123.0),
                    """{"operator":"less than","operands":[{"value":"123.0","type":"constant"}],"type":"number"}"""
                ),
                TestCase(
                    NumberConstraint.greaterThan(123.0),
                    """{"operator":"greater than","operands":[{"value":"123.0","type":"constant"}],"type":"number"}"""
                ),
                TestCase(
                    NumberConstraint.inBetween(1, 2),
                    """{"operator":"in between","operands":[{"value":"1","type":"constant"},{"value":"2","type":"constant"}],"type":"number"}""" // ktlint-disable max-line-length
                ),
                TestCase(
                    NumberConstraint.notBetween(1.123, 2.345),
                    """{"operator":"not between","operands":[{"value":"1.123","type":"constant"},{"value":"2.345","type":"constant"}],"type":"number"}""" // ktlint-disable max-line-length
                ),
                // boolean
                TestCase(
                    BooleanConstraint.isSet,
                    """{"operator":"is set","value":"true","type":"boolean"}"""
                ),
                TestCase(
                    BooleanConstraint.isNotSet,
                    """{"operator":"is not set","value":"true","type":"boolean"}"""
                ),
                TestCase(
                    BooleanConstraint.hasValue,
                    """{"operator":"has value","value":"true","type":"boolean"}"""
                ),
                TestCase(
                    BooleanConstraint.hasNoValue,
                    """{"operator":"has no value","value":"true","type":"boolean"}"""
                ),
                TestCase(
                    BooleanConstraint.itIs(false),
                    """{"operator":"is","value":"false","type":"boolean"}"""
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
            assertEquals(serializedConstraint, ExponeaGson.instance.toJson(constraint))
        }

        @Test
        fun `should deserialize`() {
            assertEquals(
                constraint,
                ExponeaGson.instance.fromJson(serializedConstraint, EventFilterConstraint::class.java)
            )
        }
    }
}
