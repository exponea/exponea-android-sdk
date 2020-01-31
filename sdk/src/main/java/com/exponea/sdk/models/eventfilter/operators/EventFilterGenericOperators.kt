package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterAttribute
import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.models.eventfilter.EventFilterOperand
import com.exponea.sdk.models.eventfilter.EventFilterOperator

internal class IsSetOperator : EventFilterOperator() {
    override val name: String = "is set"
    override val operandCount: Int = 0
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.isSet(event)
    }
}

internal class IsNotSetOperator : EventFilterOperator() {
    override val name: String = "is not set"
    override val operandCount: Int = 0
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return !attribute.isSet(event)
    }
}

internal class HasValueOperator : EventFilterOperator() {
    override val name: String = "has value"
    override val operandCount: Int = 0
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.isSet(event) && attribute.getValue(event) != null
    }
}

internal class HasNoValueOperator : EventFilterOperator() {
    override val name: String = "has no value"
    override val operandCount: Int = 0
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.isSet(event) && attribute.getValue(event) == null
    }
}
