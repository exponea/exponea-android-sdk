// ktlint-disable filename
package com.exponea.sdk.models.eventfilter.operators

import com.exponea.sdk.models.eventfilter.EventFilterAttribute
import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.models.eventfilter.EventFilterOperand
import com.exponea.sdk.models.eventfilter.EventFilterOperator

internal class IsOperator : EventFilterOperator() {
    override val name: String = "is"
    override val operandCount: Int = 1
    override fun passes(
        event: EventFilterEvent,
        attribute: EventFilterAttribute,
        operands: List<EventFilterOperand>
    ): Boolean {
        return attribute.getValue(event)?.equals(operands[0].value) == true
    }
}
