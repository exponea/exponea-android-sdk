package com.exponea.sdk.models

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class NotificationData(
    val attributes: HashMap<String, Any> = HashMap(),
    val campaignData: CampaignData = CampaignData(),
    val consentCategoryTracking: String? = null,
    val hasTrackingConsent: Boolean = true
) : Parcelable {
    val hasCustomEventType: Boolean = !((attributes["event_type"] as String?).isNullOrBlank())
    val eventType: String? = attributes["event_type"] as String?
    val sentTimestamp: Double? = attributes["sent_timestamp"] as Double?

    internal constructor(
        dataMap: HashMap<String, Any>,
        campaignMap: Map<String, String>,
        consentCategoryTracking: String?,
        hasTrackingConsent: Boolean
    ) : this(
        dataMap,
        CampaignData(campaignMap),
        consentCategoryTracking,
        hasTrackingConsent
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        writeMapToParcel(attributes, parcel)
        parcel.writeValue(campaignData)
        parcel.writeString(consentCategoryTracking ?: "")
        parcel.writeByte(if (hasTrackingConsent) 1 else 0)
    }

    @Suppress("UNCHECKED_CAST")
    private fun writeMapToParcel(mapWithNulls: Map<String, Any?>, parcel: Parcel) {
        val mapNonNullables: Map<String, Any> = mapWithNulls.filterValues { it != null } as Map<String, Any>
        parcel.writeSerializable(MAP_SIZE_EXTRA)
        parcel.writeInt(mapNonNullables.size)
        for ((key, value) in mapNonNullables.entries) {
            parcel.writeString(key)
            when (value) {
                is Map<*, *> -> {
                    writeMapToParcel(value as Map<String, Any?>, parcel)
                }
                is Array<*> -> {
                    writeArrayToParcel(value as Array<Any?>, parcel)
                }
                else -> {
                    parcel.writeSerializable(value as Serializable)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun writeArrayToParcel(arrayWithNulls: Array<Any?>, parcel: Parcel) {
        val arrayNonNullables: List<Any> = arrayWithNulls.filterNotNull()
        parcel.writeSerializable(ARRAY_SIZE_EXTRA)
        parcel.writeInt(arrayNonNullables.size)
        for (value in arrayNonNullables) {
            when (value) {
                is Map<*, *> -> {
                    writeMapToParcel(value as Map<String, Any?>, parcel)
                }
                is Array<*> -> {
                    writeArrayToParcel(value as Array<Any?>, parcel)
                }
                else -> {
                    parcel.writeSerializable(value as Serializable)
                }
            }
        }
    }

    fun getTrackingData(): Map<String, Any?> {
        val trackingData = attributes.toMutableMap()
        trackingData.remove("event_type")
        return trackingData.also {
            it.putAll(campaignData.getTrackingData())
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NotificationData> {
        private const val MAP_SIZE_EXTRA = "@MAP_SIZE_NEXT@"
        private const val ARRAY_SIZE_EXTRA = "@ARRAY_SIZE_NEXT@"

        override fun createFromParcel(parcel: Parcel): NotificationData {
            parcel.readSerializable() // read MAP_SIZE flag first
            val map: HashMap<String, Any> = readMapFromParcel(parcel)
            val campaignData = parcel.readValue(CampaignData::class.java.classLoader) as CampaignData
            val consentCategoryTracking = parcel.readString()
            val hasTrackingConsent = parcel.readByte() == 1.toByte()
            return NotificationData(
                map,
                campaignData,
                if (consentCategoryTracking.isNullOrEmpty()) null else consentCategoryTracking,
                hasTrackingConsent
            )
        }

        override fun newArray(size: Int): Array<NotificationData?> {
            return arrayOfNulls(size)
        }

        private fun readMapFromParcel(parcel: Parcel): HashMap<String, Any> {
            val mapSize = parcel.readInt()
            val map: HashMap<String, Any> = HashMap(mapSize)
            for (i in 0 until mapSize) {
                val key = parcel.readString()
                val value = parcel.readSerializable()
                if (key != null) {
                    if (value is String && value == MAP_SIZE_EXTRA) {
                        map[key] = readMapFromParcel(parcel)
                    } else if (value is String && value == ARRAY_SIZE_EXTRA) {
                        map[key] = readArrayFromParcel(parcel)
                    } else if (value != null) {
                        map[key] = value
                    }
                }
            }
            return map
        }

        private fun readArrayFromParcel(parcel: Parcel): Array<Any> {
            val size = parcel.readInt()
            val array: Array<Any> = arrayOf()
            for (i in 0 until size) {
                val value = parcel.readSerializable()
                if (value is String && value == MAP_SIZE_EXTRA) {
                    array[i] = readMapFromParcel(parcel)
                } else if (value is String && value == ARRAY_SIZE_EXTRA) {
                    array[i] = readArrayFromParcel(parcel)
                } else if (value != null) {
                    array[i] = value
                }
            }
            return array
        }
    }
}
