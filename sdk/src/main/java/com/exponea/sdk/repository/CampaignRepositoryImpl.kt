package com.exponea.sdk.repository

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CampaignClickInfo
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.Gson
import kotlin.math.abs

internal class CampaignRepositoryImpl(
    private val gson: Gson,
    private val preferences: ExponeaPreferences
) : CampaignRepository {

    private val key = "ExponeaCampaign"

    override fun set(clickInfo: CampaignClickInfo) {
        val json = gson.toJson(clickInfo)
        preferences.setString(key, json)
    }

    override fun clear(): Boolean {
        return preferences.remove(key)
    }

    override fun get(): CampaignClickInfo? {
        val eventAsJson = preferences.getString(key, "")
        val eventInfo = gson.fromJson(eventAsJson, CampaignClickInfo::class.java)
        if (eventInfo != null && abs(currentTimeSeconds() - eventInfo.createdAt) > Exponea.campaignTTL) {
            clear()
            return null
        }
        return eventInfo
    }
}
