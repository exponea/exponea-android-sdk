package com.exponea.sdk.network

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventType
import okhttp3.Call

internal interface ExponeaService {
    fun postEvent(exponeaProject: ExponeaProject, event: ExportedEventType): Call
    fun postCustomer(exponeaProject: ExponeaProject, event: ExportedEventType): Call
    fun postFetchAttributes(exponeaProject: ExponeaProject, attributesRequest: CustomerAttributesRequest): Call
    fun getBannerConfiguration(exponeaProject: ExponeaProject): Call
    fun postFetchBanner(exponeaProject: ExponeaProject, banner: Banner): Call
    fun postFetchConsents(exponeaProject: ExponeaProject): Call
    fun postCampaignClick(exponeaProject: ExponeaProject, event: ExportedEventType): Call
    fun postFetchInAppMessages(exponeaProject: ExponeaProject, customerIds: CustomerIds): Call
}
