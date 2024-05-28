package com.exponea.sdk.models

import com.exponea.sdk.models.InAppContentBlockStatus.OK
import com.exponea.sdk.models.InAppContentBlockStatus.UNKNOWN
import com.exponea.sdk.util.Logger
import com.google.gson.annotations.SerializedName
import java.util.Date

data class InAppContentBlock(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("date_filter")
    val dateFilter: DateFilter?,
    @SerializedName("frequency")
    val rawFrequency: String?,
    @SerializedName("load_priority")
    val priority: Int?,
    @SerializedName("consent_category_tracking")
    val consentCategoryTracking: String?,
    @SerializedName("content_type")
    val rawContentType: String?,
    @SerializedName("content")
    val content: Map<String, Any?>?,
    @SerializedName("placeholders")
    val placeholders: List<String>
) {

    internal var customerIds: Map<String, String?> = mapOf()

    var personalizedData: InAppContentBlockPersonalizedData? = null

    val frequency: InAppContentBlockFrequency
        get() {
            val nonmutableRawFrequency = rawFrequency
            if (nonmutableRawFrequency == null) {
                Logger.e(this, "Empty content block frequency")
                return InAppContentBlockFrequency.UNKNOWN
            } else {
                try {
                    return InAppContentBlockFrequency.valueOf(nonmutableRawFrequency.uppercase())
                } catch (e: Throwable) {
                    Logger.e(this, "Unknown content block frequency '$rawFrequency'. $e")
                    return InAppContentBlockFrequency.UNKNOWN
                }
            }
        }

    val contentType: InAppContentBlockType
        get() {
            val nonmutableRawContentType = personalizedData?.rawContentType ?: rawContentType
            if (nonmutableRawContentType == null) {
                return InAppContentBlockType.NOT_DEFINED
            } else {
                try {
                    return InAppContentBlockType.valueOf(nonmutableRawContentType.uppercase())
                } catch (e: Throwable) {
                    Logger.e(this, "Unknown content block content type '$rawContentType'. $e")
                    return InAppContentBlockType.UNKNOWN
                }
            }
        }

    val status: InAppContentBlockStatus
        get() {
            if (!isContentPersonalized()) {
                // static content block is always OK
                return OK
            }
            return personalizedData?.status ?: UNKNOWN
        }

    val hasTrackingConsent: Boolean get() = personalizedData?.hasTrackingConsent ?: true

    val htmlContent: String?
        get() {
            val validContent = personalizedData?.content ?: content
            return validContent?.get("html") as? String
        }

    fun applyDateFilter(currentTimeSeconds: Long) = dateFilter?.let { dateFilter ->
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
    } ?: true

    fun applyFrequencyFilter(displayState: InAppContentBlockDisplayState, sessionStartDate: Date): Boolean {
        when (frequency) {
            InAppContentBlockFrequency.ALWAYS ->
                return true
            InAppContentBlockFrequency.ONLY_ONCE -> {
                val shouldDisplay = displayState.displayedLast == null
                if (!shouldDisplay) {
                    Logger.i(this, "Message '${this.name}' already displayed.")
                }
                return shouldDisplay
            }
            InAppContentBlockFrequency.ONCE_PER_VISIT -> {
                val shouldDisplay = displayState.displayedLast == null ||
                    displayState.displayedLast.before(sessionStartDate)
                if (!shouldDisplay) {
                    Logger.i(this, "Message '${this.name}' already displayed in this session.")
                }
                return shouldDisplay
            }
            InAppContentBlockFrequency.UNTIL_VISITOR_INTERACTS -> {
                val shouldDisplay = displayState.interactedLast == null
                if (!shouldDisplay) {
                    Logger.i(this, "Message '${this.name}' already interacted with.")
                }
                return shouldDisplay
            }
            else -> return true
        }
    }

    fun hasFreshContent(): Boolean {
        if (!isContentPersonalized()) {
            // is static message -> valid forever
            return true
        }
        val personalizedData = personalizedData
        if (personalizedData == null) {
            // we need content to be loaded
            return false
        }
        val timeToLive = personalizedData.timeToLive ?: 0
        val loadedAt = personalizedData.loadedAt ?: Date()
        return loadedAt.time + (timeToLive * 1000) >= System.currentTimeMillis()
    }

    fun isContentPersonalized(): Boolean {
        return rawContentType == null
    }
}

enum class InAppContentBlockStatus(val value: String) {
    OK("OK"),
    NOT_MATCHED("filter_not_matched"),
    NOT_EXIST("does_not_exist"),
    UNKNOWN("UNKNOWN");
    companion object {
        fun parseValue(value: String?): InAppContentBlockStatus {
            if (value == null) {
                return UNKNOWN
            }
            var result = values().firstOrNull { enum -> enum.value == value }
            if (result == null) {
                Logger.e(this, "Unknown content block status '$value'")
                result = UNKNOWN
            }
            return result
        }
    }
}

enum class InAppContentBlockFrequency {
    ALWAYS,
    ONLY_ONCE,
    ONCE_PER_VISIT,
    UNTIL_VISITOR_INTERACTS,
    UNKNOWN
}

enum class InAppContentBlockType {
    /**
     * InApp Content Block with HTML content
     */
    HTML,

    /**
     * InApp Content Block with data to be shown with Android native UI elements
     */
    NATIVE,
    /**
     * Not yet known type; Personalized InApp Content Block has unknown content type until full fetch
     */
    NOT_DEFINED,

    /**
     * Type out of current specification.
     */
    UNKNOWN
}
