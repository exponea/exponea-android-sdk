package com.exponea.sdk.repository

import com.exponea.sdk.models.CampaignClickInfo

/**
 * Repository for storing a single CampaignClickInfo.
 */
interface CampaignRepository {
    /**
     * Returns CampaignClickInfo if exists and lives shorter than Exponea.campaignTTL
     */
    fun get(): CampaignClickInfo?

    /**
     * Store (and replace if already exists) a CampaignClickInfo.
     */
    fun set(clickInfo: CampaignClickInfo)

    /**
     * Remove CampaignClickInfo from repository if exists. Returns TRUE on success, FALSE on any error.
     */
    fun clear(): Boolean
}
