package com.exponea.sdk.network

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.CustomerExportModel
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.ExponeaFetchId
import com.exponea.sdk.models.ExponeaFetchProperty
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FetchEventsRequest
import okhttp3.Call

internal interface ExponeaService {
    fun postEvent(projectToken: String, event: ExportedEventType): Call
    fun postCustomer(projectToken: String, event: ExportedEventType): Call
    fun postRotateToken(projectToken: String): Call
    fun postRevokeToken(projectToken: String): Call
    fun postFetchProperty(projectToken: String, property: ExponeaFetchProperty): Call
    fun postFetchId(projectToken: String, id: ExponeaFetchId): Call
    fun postFetchSegmentation(projectToken: String, id: ExponeaFetchId): Call
    fun postFetchExpression(projectToken: String, id: ExponeaFetchId): Call
    fun postFetchPrediction(projectToken: String, id: ExponeaFetchId): Call
    fun postFetchRecommendation(projectToken: String, recommendation: CustomerRecommendation): Call
    fun postFetchAttributes(projectToken: String, attributes: CustomerAttributes): Call
    fun postFetchEvents(projectToken: String, events: FetchEventsRequest): Call
    fun postFetchAllProperties(projectToken: String, customerIds: CustomerIds): Call
    fun postFetchAllCustomers(projectToken: String, customer: CustomerExportModel): Call
    fun postAnonymize(projectToken: String, customerIds: CustomerIds): Call
    fun getBannerConfiguration(projectToken: String): Call
    fun postFetchBanner(projectToken: String, banner: Banner): Call
    fun postFetchConsents(projectToken: String): Call
    fun postCampaignClick(projectToken: String, event: ExportedEventType): Call
}