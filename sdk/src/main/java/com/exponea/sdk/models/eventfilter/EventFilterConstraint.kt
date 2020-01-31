package com.exponea.sdk.models.eventfilter

import com.exponea.sdk.models.eventfilter.operators.ContainsOperator
import com.exponea.sdk.models.eventfilter.operators.DoesNotContainOperator
import com.exponea.sdk.models.eventfilter.operators.DoesNotEqualOperator
import com.exponea.sdk.models.eventfilter.operators.EndsWithOperator
import com.exponea.sdk.models.eventfilter.operators.EqualToOperator
import com.exponea.sdk.models.eventfilter.operators.EqualsOperator
import com.exponea.sdk.models.eventfilter.operators.GreaterThanOperator
import com.exponea.sdk.models.eventfilter.operators.HasNoValueOperator
import com.exponea.sdk.models.eventfilter.operators.HasValueOperator
import com.exponea.sdk.models.eventfilter.operators.InBetweenOperator
import com.exponea.sdk.models.eventfilter.operators.InOperator
import com.exponea.sdk.models.eventfilter.operators.IsNotSetOperator
import com.exponea.sdk.models.eventfilter.operators.IsOperator
import com.exponea.sdk.models.eventfilter.operators.IsSetOperator
import com.exponea.sdk.models.eventfilter.operators.LessThanOperator
import com.exponea.sdk.models.eventfilter.operators.NotBetweenOperator
import com.exponea.sdk.models.eventfilter.operators.NotInOperator
import com.exponea.sdk.models.eventfilter.operators.RegexOperator
import com.exponea.sdk.models.eventfilter.operators.StartsWithOperator
import com.google.gson.annotations.SerializedName
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory

interface EventFilterConstraint {
    companion object {
        val typeAdapterFactory = RuntimeTypeAdapterFactory.of(EventFilterConstraint::class.java, "type", true)
            .registerSubtype(StringConstraint::class.java, "string")
            .registerSubtype(NumberConstraint::class.java, "number")
            .registerSubtype(BooleanConstraint::class.java, "boolean")
    }

    val type: String
    val operator: EventFilterOperator
    fun passes(event: EventFilterEvent, attribute: EventFilterAttribute): Boolean
}

data class EventFilterOperand(
    @SerializedName("value")
    val value: String
) {
    @SerializedName("type")
    val type: String = "constant"
}

data class StringConstraint(
    override val operator: EventFilterOperator,
    val operands: List<EventFilterOperand> = arrayListOf()
) : EventFilterConstraint {
    companion object {
        val isSet = StringConstraint(IsSetOperator())
        val isNotSet = StringConstraint(IsNotSetOperator())
        val hasValue = StringConstraint(HasValueOperator())
        val hasNoValue = StringConstraint(HasNoValueOperator())

        fun equals(other: String) =
            StringConstraint(EqualsOperator(), arrayListOf(EventFilterOperand(other)))
        fun doesNotEqual(other: String) =
            StringConstraint(DoesNotEqualOperator(), arrayListOf(EventFilterOperand(other)))
        fun isIn(list: List<String>) =
            StringConstraint(InOperator(), list.map { EventFilterOperand(it) })
        fun notIn(list: List<String>) =
            StringConstraint(NotInOperator(), list.map { EventFilterOperand(it) })
        fun contains(substring: String) =
            StringConstraint(ContainsOperator(), arrayListOf(EventFilterOperand(substring)))
        fun doesNotContain(substring: String) =
            StringConstraint(DoesNotContainOperator(), arrayListOf(EventFilterOperand(substring)))
        fun startsWith(prefix: String) =
            StringConstraint(StartsWithOperator(), arrayListOf(EventFilterOperand(prefix)))
        fun endsWith(suffix: String) =
            StringConstraint(EndsWithOperator(), arrayListOf(EventFilterOperand(suffix)))
        fun regex(regex: String) =
            StringConstraint(RegexOperator(), arrayListOf(EventFilterOperand(regex)))
    }

    override val type: String = "string"

    override fun passes(event: EventFilterEvent, attribute: EventFilterAttribute): Boolean {
        return operator.validate(operands) && operator.passes(event, attribute, operands)
    }

    override fun equals(other: Any?): Boolean {
        return other is StringConstraint && other.operator == operator && other.operands == operands
    }
}

data class NumberConstraint(
    override val operator: EventFilterOperator,
    val operands: List<EventFilterOperand> = arrayListOf()
) : EventFilterConstraint {
    companion object {
        val isSet = NumberConstraint(IsSetOperator())
        val isNotSet = NumberConstraint(IsNotSetOperator())
        val hasValue = NumberConstraint(HasValueOperator())
        val hasNoValue = NumberConstraint(HasNoValueOperator())

        fun equalTo(other: Number) =
            NumberConstraint(EqualToOperator(), arrayListOf(EventFilterOperand(other.toString())))
        fun lessThan(other: Number) =
            NumberConstraint(LessThanOperator(), arrayListOf(EventFilterOperand(other.toString())))
        fun greaterThan(other: Number) =
            NumberConstraint(GreaterThanOperator(), arrayListOf(EventFilterOperand(other.toString())))
        fun inBetween(start: Number, end: Number) =
            NumberConstraint(
                InBetweenOperator(),
                arrayListOf(EventFilterOperand(start.toString()), EventFilterOperand(end.toString()))
            )
        fun notBetween(start: Number, end: Number) =
            NumberConstraint(
            NotBetweenOperator(),
            arrayListOf(EventFilterOperand(start.toString()), EventFilterOperand(end.toString()))
        )
    }

    override val type: String = "number"

    override fun passes(event: EventFilterEvent, attribute: EventFilterAttribute): Boolean {
        return operator.validate(operands) && operator.passes(event, attribute, operands)
    }

    override fun equals(other: Any?): Boolean {
        return other is NumberConstraint && other.operator == operator && other.operands == operands
    }
}

data class BooleanConstraint(
    override val operator: EventFilterOperator,
    val value: String = "true"
) : EventFilterConstraint {
    companion object {
        val isSet = BooleanConstraint(IsSetOperator())
        val isNotSet = BooleanConstraint(IsNotSetOperator())
        val hasValue = BooleanConstraint(HasValueOperator())
        val hasNoValue = BooleanConstraint(HasNoValueOperator())

        fun itIs(value: Boolean) = BooleanConstraint(IsOperator(), value.toString())
    }

    override val type: String = "boolean"

    override fun passes(event: EventFilterEvent, attribute: EventFilterAttribute): Boolean {
        return operator.passes(event, attribute, arrayListOf(EventFilterOperand(value = value)))
    }

    override fun equals(other: Any?): Boolean {
        return other is BooleanConstraint && other.operator == operator && other.value == value
    }
}
