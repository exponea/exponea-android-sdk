package com.exponea.sdk.util

import android.net.Uri

internal object GdprTracking {
    fun isTrackForced(url: String?): Boolean = runCatching {
        if (url == null) {
            return false
        }
        val uri = Uri.parse(url)
        val rawForceTrack = uri.getQueryParameter("xnpe_force_track")
        if (rawForceTrack == null) {
            return false
        }
        if (rawForceTrack.isEmpty()) {
            // URI RFC doesn't mandate that query param needs to be name-value pair
            // this IF is handling of case '?key=value&xnpe_force_track'
            return true
        }
        return isTrueString(rawForceTrack)
    }.getOrElse {
        Logger.e(this, "Action url cannot be checked for force-track value")
        return false
    }

    /**
     * Checks if given value has TRUE value according to possible values of "has_tracking_content" specification
     * including values as 'true' or '1' etc...
     */
    fun hasTrackingConsent(source: Any?): Boolean {
        if (source == null) {
            // default behaviour is that consent is given
            return true
        }
        when (source) {
            is Boolean -> return source
            is Int -> return source == 1
            is String -> return isTrueString(source)
            else -> {
                Logger.e(this, "Tracking consent cannot be determined from $source")
                return false
            }
        }
    }

    private fun isTrueString(source: String): Boolean = when (source.lowercase()) {
        "true" -> true
        "1" -> true
        "false" -> false
        "0" -> false
        else -> {
            Logger.e(this,
                "Unpredicted value '$source' to check boolean value")
            false
        }
    }
}
