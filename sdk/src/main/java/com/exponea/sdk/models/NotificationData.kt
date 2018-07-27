package com.exponea.sdk.models

import android.os.Parcel
import android.os.Parcelable

data class NotificationData(
        val campaignId: String? = null,
        val campaignName: String? = null,
        val actionId: Long? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readValue(Long::class.java.classLoader) as? Long)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(campaignId)
        parcel.writeString(campaignName)
        parcel.writeValue(actionId)
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