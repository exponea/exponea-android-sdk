package com.exponea.sdk.network

import com.exponea.sdk.models.ApiEndPoint
import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.CampaignClickEvent
import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.ExportedEventType
import com.google.gson.Gson
import okhttp3.Call

internal class ExponeaServiceImpl(
    private val gson: Gson,
    private val networkManager: NetworkHandler
) : ExponeaService {

    override fun postCampaignClick(exponeaProject: ExponeaProject, event: ExportedEventType): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.TRACK_CAMPAIGN, CampaignClickEvent(event))
    }

    override fun postEvent(exponeaProject: ExponeaProject, event: ExportedEventType): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.TRACK_EVENTS, event)
    }

    override fun postCustomer(exponeaProject: ExponeaProject, event: ExportedEventType): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.TRACK_CUSTOMERS, event)
    }

    override fun postFetchAttributes(
        exponeaProject: ExponeaProject,
        attributesRequest: CustomerAttributesRequest
    ): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES, attributesRequest)
    }

    override fun getBannerConfiguration(exponeaProject: ExponeaProject): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CONFIGURE_BANNER,
                exponeaProject.projectToken
        ).toString()
        return networkManager.get(exponeaProject.baseUrl + endPoint, exponeaProject.authorization)
    }

    override fun postFetchBanner(exponeaProject: ExponeaProject, banner: Banner): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.SHOW_BANNER, banner)
    }

    override fun postFetchConsents(exponeaProject: ExponeaProject): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.CONSENTS, null)
    }

    override fun postFetchInAppMessages(exponeaProject: ExponeaProject, customerIds: CustomerIds): Call {
        return doPost(
            exponeaProject,
            ApiEndPoint.EndPointName.IN_APP_MESSAGES,
            hashMapOf(
                "customer_ids" to customerIds.toHashMap(),
                "device" to "android"
            )
        )
    }

    private fun doPost(
        exponeaProject: ExponeaProject,
        endPointName: ApiEndPoint.EndPointName,
        bodyContent: Any?
    ): Call {
        val endpoint = ApiEndPoint(endPointName, exponeaProject.projectToken).toString()
        val jsonBody = bodyContent?.let { gson.toJson(it) }
        return networkManager.post(exponeaProject.baseUrl + endpoint, exponeaProject.authorization, jsonBody)
    }
}
