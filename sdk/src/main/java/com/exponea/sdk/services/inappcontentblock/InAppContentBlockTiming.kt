package com.exponea.sdk.services.inappcontentblock

import android.os.SystemClock
import com.exponea.sdk.util.Logger
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Emits independent, machine-readable content-block lifecycle events. */
internal object InAppContentBlockTiming {

    internal enum class RequestSource(val logValue: String) {
        CONTENT_FRESH_CHECKED("content_fresh_checked"),
        ETAG_LOOKED_UP("etag_looked_up"),
        NETWORK_FETCH_STARTED("network_fetch_started"),
        NETWORK_FETCH_FINISHED("network_fetch_finished"),
        CONTENT_FILTERED("content_filtered"),
        CONTENT_SELECTED("content_selected"),
        CAROUSEL_RELOADED("carousel_reloaded"),
        CAROUSEL_ITEM_BOUND("carousel_item_bound"),
        CONTENT_LOADED("content_loaded"),
        NORMALIZATION_FINISHED("normalization_finished"),
        DISPLAY_STARTED("display_started"),
        RENDER_FINISHED("render_finished")
    }

    internal enum class EtagMode(val logValue: String) {
        NONE("none"),
        SENT("sent"),
        NOT_MODIFIED("not_modified"),
        STORED("stored"),
        RETRY_WITHOUT_ETAG("retry_without_etag"),
        FAILED("failed")
    }

    internal enum class NormalizationMode(val logValue: String) {
        CACHE_HIT("cache_hit"),
        CACHE_MISS("cache_miss"),
        INVALID("invalid"),
        NOT_HTML("not_html")
    }

    fun log(
        contentBlockId: String,
        source: RequestSource,
        contentBlockName: String? = null,
        etagMode: EtagMode? = null,
        normalizationMode: NormalizationMode? = null,
        properties: Map<String, Any?> = emptyMap()
    ) {
        if (Logger.level.value > Logger.Level.DEBUG.value) {
            return
        }

        val fields = mutableListOf(
            "contentBlockId=${encode(contentBlockId)}",
            "timestampMs=${SystemClock.elapsedRealtime()}",
            "source=${source.logValue}"
        )
        contentBlockName?.let { fields.add("contentBlockName=${encode(it)}") }
        etagMode?.let { fields.add("etagMode=${it.logValue}") }
        normalizationMode?.let { fields.add("normalizationMode=${it.logValue}") }
        properties.toSortedMap().forEach { (key, value) ->
            value?.let { fields.add("$key=${encode(it.toString())}") }
        }
        Logger.d(this, "InAppCB timing: ${fields.joinToString(", ")}")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
