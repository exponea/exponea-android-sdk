package com.exponea.sdk.repository

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CampaignData
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.Gson
import kotlin.math.abs

internal class CampaignRepositoryImpl(
    private val gson: Gson,
    private val preferences: ExponeaPreferences
) : CampaignRepository {

    private val key = "ExponeaCampaign"

    override fun set(campaignData: CampaignData) {
        if (Exponea.isStopped) {
            Logger.e(this, "Campaign event not stored, SDK is stopping")
            return
        }
        val json = gson.toJson(campaignData)
        preferences.setString(key, json)
    }

    override fun clear(): Boolean {
        return preferences.remove(key)
    }

    override fun get(): CampaignData? {
        if (Exponea.isStopped) {
            Logger.e(this, "Campaign event not loaded, SDK is stopping")
            return null
        }
        val data = gson.fromJson(preferences.getString(key, ""), CampaignData::class.java)
        if (data != null && abs(currentTimeSeconds() - data.createdAt) > Exponea.campaignTTL) {
            clear()
            return null
        }
        return data
    }

    override fun onIntegrationStopped() {
        clear()
    }
}
