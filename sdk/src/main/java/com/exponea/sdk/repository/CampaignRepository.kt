package com.exponea.sdk.repository

import com.exponea.sdk.models.CampaignData

/**
 * Repository for storing a single CampaignData.
 */
internal interface CampaignRepository {
    /**
     * Returns CampaignData if exists and lives shorter than Exponea.campaignTTL
     */
    fun get(): CampaignData?

    /**
     * Store (and replace if already exists) a CampaignData.
     */
    fun set(campaignData: CampaignData)

    /**
     * Remove CampaignData from repository if exists. Returns TRUE on success, FALSE on any error.
     */
    fun clear(): Boolean
}
