package com.exponea.sdk.network

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.ExportedEventType
import okhttp3.Call

internal interface ExponeaService {
    fun postEvent(projectToken: String, event: ExportedEventType): Call
    fun postCustomer(projectToken: String, event: ExportedEventType): Call
    fun postFetchRecommendation(projectToken: String, recommendation: CustomerRecommendation): Call
    fun getBannerConfiguration(projectToken: String): Call
    fun postFetchBanner(projectToken: String, banner: Banner): Call
    fun postFetchConsents(projectToken: String): Call
    fun postCampaignClick(projectToken: String, event: ExportedEventType): Call
}
