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
        return when (rawForceTrack.lowercase()) {
            "true" -> true
            "1" -> true
            "false" -> false
            "0" -> false
            else -> {
                Logger.e(this,
                    "Action url contains force-track with incompatible value $rawForceTrack")
                return false
            }
        }
    }.getOrElse {
        Logger.e(this, "Action url cannot be checked for force-track value")
        return false
    }
}
