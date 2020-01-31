package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterAttribute
import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.models.eventfilter.EventFilterOperand
import com.exponea.sdk.models.eventfilter.EventFilterOperator

internal class EqualsOperator : EventFilterOperator() {
    override val name: String = "equals"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.equals(operands[0].value) == true
    }
}

internal class DoesNotEqualOperator : EventFilterOperator() {
    override val name: String = "does not equal"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.equals(operands[0].value) == false
    }
}

internal class InOperator : EventFilterOperator() {
    override val name: String = "in"
    override val operandCount: Int = ANY_OPERAND_COUNT
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        val value: String = attribute.getValue(event) ?: return false
        return operands.any { it.value == value }
    }
}

internal class NotInOperator : EventFilterOperator() {
    override val name: String = "not in"
    override val operandCount: Int = ANY_OPERAND_COUNT
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        val value: String = attribute.getValue(event) ?: return false
        return operands.all { it.value != value }
    }
}

internal class ContainsOperator : EventFilterOperator() {
    override val name: String = "contains"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.contains(operands[0].value) == true
    }
}

internal class DoesNotContainOperator : EventFilterOperator() {
    override val name: String = "does not contain"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.contains(operands[0].value) == false
    }
}

internal class StartsWithOperator : EventFilterOperator() {
    override val name: String = "starts with"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.startsWith(operands[0].value) == true
    }
}

internal class EndsWithOperator : EventFilterOperator() {
    override val name: String = "ends with"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.endsWith(operands[0].value) == true
    }
}

internal class RegexOperator : EventFilterOperator() {
    override val name: String = "regex"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.contains(Regex(operands[0].value)) == true
    }
}
