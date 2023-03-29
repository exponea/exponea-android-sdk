package com.exponea.sdk.manager

import com.exponea.sdk.models.CampaignData

internal interface CampaignManager {
    fun trackCampaignClick(campaignData: CampaignData)
}
