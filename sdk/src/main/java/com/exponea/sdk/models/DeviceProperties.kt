package com.exponea.sdk.models

import android.os.Build
import com.google.gson.annotations.SerializedName

data class DeviceProperties(
        var campaign: String? = null,
        @SerializedName("campaign_id")
        var campaignId: String? = null,
        var link: String? = null,
        var osName: String = Constants.DeviceInfo.osName,
        var osVersion: String = Build.VERSION.RELEASE,
        var sdk: String = Constants.DeviceInfo.sdk,
        var sdkVersion: String = Constants.DeviceInfo.sdkVersion,
        var deviceModel: String = Build.MODEL,
        var deviceType: String? = null
) {
    fun toHashMap(): HashMap<String, Any> {
        val hashMap = hashMapOf<String, Any>(
                Pair("os_name", osName),
                Pair("os_version", osVersion),
                Pair("sdk", sdk),
                Pair("sdk_version", sdkVersion),
                Pair("device_model", deviceModel)
        )

        campaign?.let { hashMap["campaign"] = it }
        campaignId?.let { hashMap["campaign_id"] = it }
        link?.let { hashMap["link"] = it }
        deviceType?.let { hashMap["device_type"] = it }

        return hashMap
    }
}