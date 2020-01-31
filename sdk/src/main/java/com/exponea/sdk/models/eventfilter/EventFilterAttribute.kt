package com.exponea.sdk.models.eventfilter

import com.google.gson.annotations.SerializedName
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory

interface EventFilterAttribute {
    companion object {
        internal val typeAdapterFactory = RuntimeTypeAdapterFactory.of(EventFilterAttribute::class.java, "type", true)
            .registerSubtype(PropertyAttribute::class.java, "property")
            .registerSubtype(TimestampAttribute::class.java, "timestamp")
    }

    val type: String
    fun isSet(event: EventFilterEvent): Boolean
    fun getValue(event: EventFilterEvent): String?
}

class TimestampAttribute : EventFilterAttribute {
    @SerializedName("type")
    override val type: String = "timestamp"
    override fun getValue(event: EventFilterEvent): String? = event.timestamp?.toString()
    override fun isSet(event: EventFilterEvent): Boolean = event.timestamp != null

    override fun equals(other: Any?): Boolean {
        return other is TimestampAttribute
    }

    override fun hashCode(): Int {
        return 1
    }
}

data class PropertyAttribute(
    @SerializedName("property")
    val property: String
) : EventFilterAttribute {
    @SerializedName("type")
    override val type: String = "property"
    override fun getValue(event: EventFilterEvent): String? = event.properties[property]?.toString()
    override fun isSet(event: EventFilterEvent): Boolean = event.properties.containsKey(property)
}
