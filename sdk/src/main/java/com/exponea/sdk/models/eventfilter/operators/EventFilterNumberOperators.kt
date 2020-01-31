package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterAttribute
import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.models.eventfilter.EventFilterOperand
import com.exponea.sdk.models.eventfilter.EventFilterOperator

internal class EqualToOperator : EventFilterOperator() {
    override val name: String = "equal to"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return try {
            (attribute.getValue(event) ?: return false).toDouble() == operands[0].value.toDouble()
        } catch (e: NumberFormatException) {
            false
        }
    }
}

internal class InBetweenOperator : EventFilterOperator() {
    override val name: String = "in between"
    override val operandCount: Int = 2
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        try {
            val value: Double = (attribute.getValue(event) ?: return false).toDouble()
            return value >= operands[0].value.toDouble() && value <= operands[1].value.toDouble()
        } catch (e: NumberFormatException) {
            return false
        }
    }
}

internal class NotBetweenOperator : EventFilterOperator() {
    override val name: String = "not between"
    override val operandCount: Int = 2
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        try {
            val value: Double = (attribute.getValue(event) ?: return false).toDouble()
            return value < operands[0].value.toDouble() || value > operands[1].value.toDouble()
        } catch (e: NumberFormatException) {
            return false
        }
    }
}

internal class LessThanOperator : EventFilterOperator() {
    override val name: String = "less than"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return try {
            (attribute.getValue(event) ?: return false).toDouble() < operands[0].value.toDouble()
        } catch (e: NumberFormatException) {
            false
        }
    }
}

internal class GreaterThanOperator : EventFilterOperator() {
    override val name: String = "greater than"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return try {
            (attribute.getValue(event) ?: return false).toDouble() > operands[0].value.toDouble()
        } catch (e: NumberFormatException) {
            false
        }
    }
}
