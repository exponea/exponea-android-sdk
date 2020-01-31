package com.exponea.sdk.models.eventfilter

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

data class EventFilterEvent(
    val eventType: String,
    val properties: Map<String, Any?>,
    val timestamp: Double?
)

data class EventFilter(
    @SerializedName("type")
    val eventType: String,
    @SerializedName("filter")
    val filter: List<EventPropertyFilter>
) {
    companion object {
        internal val gson = GsonBuilder()
            .registerTypeHierarchyAdapter(EventFilterOperator::class.java, EventFilterOperatorSerializer())
            .registerTypeHierarchyAdapter(EventFilterOperator::class.java, EventFilterOperatorDeserializer())
            .registerTypeAdapterFactory(EventFilterAttribute.typeAdapterFactory)
            .registerTypeAdapterFactory(EventFilterConstraint.typeAdapterFactory)
            .create()

        internal fun deserialize(data: String): EventFilter = gson.fromJson(data, EventFilter::class.java)
    }

    fun passes(event: EventFilterEvent): Boolean {
        if (event.eventType != eventType) return false
        return filter.all { it.passes(event) }
    }

    fun serialize(): String = gson.toJson(this)
}

data class EventPropertyFilter(
    @SerializedName("attribute")
    val attribute: EventFilterAttribute,
    @SerializedName("constraint")
    val constraint: EventFilterConstraint
) {
    companion object {
        fun timestamp(constraint: EventFilterConstraint) =
            EventPropertyFilter(TimestampAttribute(), constraint)
        fun property(name: String, constraint: EventFilterConstraint) =
            EventPropertyFilter(PropertyAttribute(name), constraint)
    }
    fun passes(event: EventFilterEvent): Boolean {
        return constraint.passes(event, attribute)
    }
}
