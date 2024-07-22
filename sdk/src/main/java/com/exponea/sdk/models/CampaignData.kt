package com.exponea.sdk.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.exponea.sdk.util.currentTimeSeconds

/**
 * Campaign data retrieved from Android Deeplink Intent Uri or push notifications.
 */
data class CampaignData(
    var source: String? = null,
    var campaign: String? = null,
    var content: String? = null,
    var medium: String? = null,
    var term: String? = null,
    var payload: String? = null,
    var createdAt: Double = currentTimeSeconds(),
    var completeUrl: String? = null
) : Parcelable {
    internal constructor(campaignMap: Map<String, String>) : this(
        source = campaignMap["utm_source"],
        campaign = campaignMap["utm_campaign"],
        content = campaignMap["utm_content"],
        medium = campaignMap["utm_medium"],
        term = campaignMap["utm_term"],
        payload = campaignMap["xnpe_cmp"],
        createdAt = currentTimeSeconds(),
        completeUrl = null
    )

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

    fun isValidForCampaignTrack(): Boolean = payload?.isNotEmpty() == true

    fun getTrackingData(): Map<String, String> {
        return hashMapOf(
            "url" to completeUrl,
            "location" to completeUrl, // we used to track this in session start, we should keep it around
            "utm_source" to source,
            "utm_medium" to medium,
            "utm_campaign" to campaign,
            "utm_content" to content,
            "utm_term" to term,
            "xnpe_cmp" to payload
        ).filterValues { it != null }.mapValues { it.value as String }
    }

    companion object CREATOR : Parcelable.Creator<CampaignData> {
        override fun createFromParcel(parcel: Parcel): CampaignData {
            return CampaignData(parcel)
        }

        override fun newArray(size: Int): Array<CampaignData?> {
            return arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        source = parcel.readString(),
        campaign = parcel.readString(),
        content = parcel.readString(),
        medium = parcel.readString(),
        term = parcel.readString(),
        payload = parcel.readString(),
        createdAt = parcel.readDouble(),
        completeUrl = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(source)
        parcel.writeString(campaign)
        parcel.writeString(content)
        parcel.writeString(medium)
        parcel.writeString(term)
        parcel.writeString(payload)
        parcel.writeDouble(createdAt)
        parcel.writeString(completeUrl)
    }

    override fun describeContents(): Int {
        return 0
    }
}
