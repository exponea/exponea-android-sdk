package com.exponea.sdk.models

import com.exponea.sdk.models.eventfilter.EventFilter
import com.exponea.sdk.models.eventfilter.EventFilterEvent
import com.exponea.sdk.util.Logger
import com.google.gson.annotations.SerializedName
import java.util.Date

internal data class InAppMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("message_type")
    val rawMessageType: String,
    @SerializedName("frequency")
    val rawFrequency: String,
    @SerializedName("payload")
    val payload: InAppMessagePayload?,
    @SerializedName("variant_id")
    val variantId: Int,
    @SerializedName("variant_name")
    val variantName: String,
    @SerializedName("trigger")
    val trigger: EventFilter?,
    @SerializedName("date_filter")
    val dateFilter: DateFilter,
    @SerializedName("load_priority")
    val priority: Int?,
    @SerializedName("load_delay")
    val delay: Long?,
    @SerializedName("close_timeout")
    val timeout: Long?
) {
    val frequency: InAppMessageFrequency?
        get() {
            return try {
                InAppMessageFrequency.valueOf(rawFrequency.uppercase())
            } catch (e: Throwable) {
                Logger.e(this, "Unknown in-app-message frequency $rawFrequency. $e")
                null
            }
        }

    val messageType: InAppMessageType
        get() {
            return try {
                InAppMessageType.valueOf(rawMessageType.uppercase())
            } catch (e: Throwable) {
                Logger.e(this, "Unknown in-app-message type $rawMessageType. $e")
                InAppMessageType.MODAL
            }
        }

    fun applyDateFilter(currentTimeSeconds: Long): Boolean {
        if (!dateFilter.enabled) {
            return true
        }
        if (dateFilter.fromDate != null && dateFilter.fromDate > currentTimeSeconds) {
            Logger.i(this, "Message '${this.name}' outside of date range.")
            return false
        }
        if (dateFilter.toDate != null && dateFilter.toDate < currentTimeSeconds) {
            Logger.i(this, "Message '${this.name}' outside of date range.")
            return false
        }
        return true
    }

    fun applyEventFilter(eventType: String, properties: Map<String, Any?>, timestamp: Double?): Boolean {
        try {
            if (trigger == null) {
                Logger.i(this, "No event trigger found for message '${this.name}'.")
                return false
            }
            val passes = trigger.passes(EventFilterEvent(eventType, properties, timestamp))
            if (!passes) {
                Logger.i(
                    this,
                    "Message '${this.name}' failed event filter. " +
                        "Message filter: ${this.trigger.serialize()} " +
                        "Event type: $eventType properties: $properties timestamp: $timestamp"
                )
            }
            return passes
        } catch (e: Throwable) {
            Logger.e(this, "Applying event filter for message '${this.name}' failed. $e")
            return false
        }
    }

    fun applyFrequencyFilter(displayState: InAppMessageDisplayState, sessionStartDate: Date): Boolean {
        when (frequency) {
            InAppMessageFrequency.ALWAYS ->
                return true
            InAppMessageFrequency.ONLY_ONCE -> {
                val shouldDisplay = displayState.displayed == null
                if (!shouldDisplay) {
                    Logger.i(this, "Message '${this.name}' already displayed.")
                }
                return shouldDisplay
            }
            InAppMessageFrequency.ONCE_PER_VISIT -> {
                val shouldDisplay = displayState.displayed == null || displayState.displayed.before(sessionStartDate)
                if (!shouldDisplay) {
                    Logger.i(this, "Message '${this.name}' already displayed in this session.")
                }
                return shouldDisplay
            }
            InAppMessageFrequency.UNTIL_VISITOR_INTERACTS -> {
                val shouldDisplay = displayState.interacted == null
                if (!shouldDisplay) {
                    Logger.i(this, "Message '${this.name}' already interacted with.")
                }
                return shouldDisplay
            }
            else -> return true
        }
    }
}

enum class InAppMessageFrequency(val value: String) {
    ALWAYS("always"),
    ONLY_ONCE("only_once"),
    ONCE_PER_VISIT("once_per_visit"),
    UNTIL_VISITOR_INTERACTS("until_visitor_interacts")
}
