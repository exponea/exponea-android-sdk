package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterOperand
import com.exponea.sdk.models.eventfilter.EventFilterOperator
import com.exponea.sdk.models.eventfilter.EventFilterOperatorException
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class EventFilterOperatorsOperandCountTest(
    val operatorName: String,
    val operator: EventFilterOperator,
    val operandsCount: Int,
    val valid: Boolean
) {
    companion object {
        data class TestCase(
            val operator: EventFilterOperator,
            val operandsValidity: Map<Int, Boolean>
        )

        val testCases = arrayListOf(
            // generic
            TestCase(IsSetOperator(), hashMapOf(0 to true, 1 to false)),
            TestCase(IsNotSetOperator(), hashMapOf(0 to true, 1 to false)),
            TestCase(HasValueOperator(), hashMapOf(0 to true, 1 to false)),
            TestCase(HasNoValueOperator(), hashMapOf(0 to true, 1 to false)),
            // string
            TestCase(EqualsOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(DoesNotEqualOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(InOperator(), hashMapOf(0 to true, 1 to true, 2 to true, 10 to true)),
            TestCase(NotInOperator(), hashMapOf(0 to true, 1 to true, 2 to true, 10 to true)),
            TestCase(ContainsOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(DoesNotContainOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(StartsWithOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(EndsWithOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(RegexOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            // boolean
            TestCase(IsOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            // number
            TestCase(EqualToOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(InBetweenOperator(), hashMapOf(0 to false, 1 to false, 2 to true, 3 to false)),
            TestCase(NotBetweenOperator(), hashMapOf(0 to false, 1 to false, 2 to true, 3 to false)),
            TestCase(LessThanOperator(), hashMapOf(0 to false, 1 to true, 2 to false)),
            TestCase(GreaterThanOperator(), hashMapOf(0 to false, 1 to true, 2 to false))
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Using {2} operands on \"{0}\" operator")
        fun data(): List<Array<out Any?>> {
            val result = arrayListOf<Array<out Any?>>()
            testCases.forEach { testCase ->
                testCase.operandsValidity.forEach {
                    result.add(arrayOf(testCase.operator.name, testCase.operator, it.key, it.value))
                }
            }
            return result
        }
    }

    @Test
    fun test() {
        val operands = arrayListOf<EventFilterOperand>()
        for (n in 1..operandsCount) operands.add(EventFilterOperand("test"))
        try {
            operator.validate(operands)
        } catch (e: EventFilterOperatorException) {
            if (valid) fail("Should be valid")
            return
        }
        if (!valid) fail("Should be invalid")
    }
}
