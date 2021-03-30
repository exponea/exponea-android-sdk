package com.exponea.sdk.models

import android.os.Parcel
import android.os.Parcelable

data class NotificationData(
    val eventType: String? = null,
    val campaignId: String? = null,
    val campaignName: String? = null,
    val actionId: Long? = null,
    val actionName: String? = null,
    val actionType: String? = null,
    val campaignPolicy: String? = null,
    val platform: String? = null,
    val language: String? = null,
    val recipient: String? = null,
    val subject: String? = null,
    val sentTimestamp: Double? = null,
    val type: String? = null,
    val campaignData: CampaignData = CampaignData()
) : Parcelable {
    val hasCustomEventType: Boolean = !(eventType?.isBlank() ?: true)

    internal constructor(dataMap: Map<String, Any>, campaignMap: Map<String, String>) : this(
        dataMap["event_type"] as? String,
        dataMap["campaign_id"] as? String,
        dataMap["campaign_name"] as? String,
        (dataMap["action_id"] as? Double)?.toLong(),
        dataMap["action_name"] as? String,
        dataMap["action_type"] as? String,
        dataMap["campaign_policy"] as? String,
        dataMap["platform"] as? String,
        dataMap["language"] as? String,
        dataMap["recipient"] as? String,
        dataMap["subject"] as? String,
        dataMap["sent_timestamp"] as? Double,
        dataMap["type"] as? String,
        CampaignData(campaignMap)
    )

    constructor(parcel: Parcel) : this(
        eventType = parcel.readString(),
        campaignId = parcel.readString(),
        campaignName = parcel.readString(),
        actionId = parcel.readValue(Long::class.java.classLoader) as? Long,
        actionName = parcel.readString(),
        actionType = parcel.readString(),
        campaignPolicy = parcel.readString(),
        platform = parcel.readString(),
        language = parcel.readString(),
        recipient = parcel.readString(),
        subject = parcel.readString(),
        sentTimestamp = parcel.readValue(Double::class.java.classLoader) as? Double,
        type = parcel.readString(),
        campaignData = parcel.readValue(CampaignData::class.java.classLoader) as CampaignData
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(eventType)
        parcel.writeString(campaignId)
        parcel.writeString(campaignName)
        parcel.writeValue(actionId)
        parcel.writeString(actionName)
        parcel.writeString(actionType)
        parcel.writeString(campaignPolicy)
        parcel.writeString(platform)
        parcel.writeString(language)
        parcel.writeString(recipient)
        parcel.writeString(subject)
        parcel.writeValue(sentTimestamp)
        parcel.writeString(type)
        parcel.writeValue(campaignData)
    }

    fun getTrackingData(): Map<String, Any?> {
        return hashMapOf(
            "campaign_id" to campaignId,
            "campaign_name" to campaignName,
            "action_id" to actionId,
            "action_name" to actionName,
            "action_type" to actionType,
            "campaign_policy" to campaignPolicy,
            "platform" to platform,
            "language" to language,
            "recipient" to recipient,
            "subject" to subject,
            "sent_timestamp" to sentTimestamp,
            "type" to type
        ).also {
            it.putAll(campaignData.getTrackingData())
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NotificationData> {
        override fun createFromParcel(parcel: Parcel): NotificationData {
            return NotificationData(parcel)
        }

        override fun newArray(size: Int): Array<NotificationData?> {
            return arrayOfNulls(size)
        }
    }
}
