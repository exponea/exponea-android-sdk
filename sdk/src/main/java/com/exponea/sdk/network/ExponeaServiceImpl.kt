package com.exponea.sdk.network

import com.exponea.sdk.models.ApiEndPoint
import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.CampaignClickEvent
import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExportedEventType
import com.google.gson.Gson
import okhttp3.Call

internal class ExponeaServiceImpl(
    private val gson: Gson,
    private val networkManager: NetworkHandler
) : ExponeaService {

    override fun postCampaignClick(projectToken: String, event: ExportedEventType): Call {
        return doPost(ApiEndPoint.EndPointName.TRACK_CAMPAIGN, projectToken, CampaignClickEvent(event))
    }

    override fun postEvent(projectToken: String, event: ExportedEventType): Call {
        return doPost(ApiEndPoint.EndPointName.TRACK_EVENTS, projectToken, event)
    }

    override fun postCustomer(projectToken: String, event: ExportedEventType): Call {
        return doPost(ApiEndPoint.EndPointName.TRACK_CUSTOMERS, projectToken, event)
    }

    override fun postFetchAttributes(
        projectToken: String,
        attributesRequest: CustomerAttributesRequest
    ): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES, projectToken, attributesRequest)
    }

    override fun getBannerConfiguration(projectToken: String): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CONFIGURE_BANNER,
                projectToken
        ).toString()
        return networkManager.get(endPoint, null)
    }

    override fun postFetchBanner(projectToken: String, banner: Banner): Call {
        return doPost(ApiEndPoint.EndPointName.SHOW_BANNER, projectToken, banner)
    }

    override fun postFetchConsents(projectToken: String): Call {
        return doPost(ApiEndPoint.EndPointName.CONSENTS, projectToken, null)
    }

    override fun postFetchInAppMessages(projectToken: String, customerIds: CustomerIds): Call {
        return doPost(
            ApiEndPoint.EndPointName.IN_APP_MESSAGES,
            projectToken,
            hashMapOf(
                "customer_ids" to customerIds.toHashMap(),
                "device" to "android"
            )
        )
    }

    private fun doPost(
        endPointName: ApiEndPoint.EndPointName,
        projectToken: String,
        bodyContent: Any?
    ): Call {
        val endpoint = ApiEndPoint(endPointName, projectToken).toString()
        val jsonBody = bodyContent?.let { gson.toJson(it) }
        return networkManager.post(endpoint, jsonBody)
    }
}
