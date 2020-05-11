package com.exponea.sdk.models

import android.net.Uri
import android.text.TextUtils
import com.exponea.sdk.util.currentTimeSeconds

/**
 * Info holder retrieved from Android Deeplink Intent Uri and push notifications.
 */
internal data class CampaignClickInfo(
    var source: String?,
    var campaign: String?,
    var content: String?,
    var medium: String?,
    var term: String?,
    var payload: String?,
    val createdAt: Double = currentTimeSeconds(),
    val completeUrl: String
) {
    internal constructor(data: Uri) : this(
            source = data.getQueryParameter("utm_source"),
            campaign = data.getQueryParameter("utm_campaign"),
            content = data.getQueryParameter("utm_content"),
            medium = data.getQueryParameter("utm_medium"),
            term = data.getQueryParameter("utm_term"),
            payload = data.getQueryParameter("xnpe_cmp"),
            createdAt = currentTimeSeconds(),
            completeUrl = data.toString()
    )

    fun isValid() = !TextUtils.isEmpty(payload)

    fun getTrackingData(): Map<String, String> {
        return hashMapOf(
            "url" to completeUrl,
            "location" to completeUrl, // we used to track this in session start, we should keep it around
            "utm_source" to source.orEmpty(),
            "utm_medium" to medium.orEmpty(),
            "utm_campaign" to campaign.orEmpty(),
            "utm_content" to content.orEmpty(),
            "utm_term" to term.orEmpty()
        )
    }
}
