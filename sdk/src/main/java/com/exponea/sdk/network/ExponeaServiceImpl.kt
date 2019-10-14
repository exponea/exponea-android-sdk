package com.exponea.sdk.network

import com.exponea.sdk.models.ApiEndPoint
import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.CampaignClickEvent
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.CustomerExportModel
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.ExponeaFetchId
import com.exponea.sdk.models.ExponeaFetchProperty
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FetchEventsRequest
import com.google.gson.Gson
import okhttp3.Call

class ExponeaServiceImpl(
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

    override fun postRotateToken(projectToken: String): Call {
        return doPost(ApiEndPoint.EndPointName.TOKEN_ROTATE, projectToken, null)
    }

    override fun postRevokeToken(projectToken: String): Call {
        return doPost(ApiEndPoint.EndPointName.TOKEN_REVOKE, projectToken, null)
    }

    override fun postFetchProperty(projectToken: String, property: ExponeaFetchProperty): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_PROPERTY, projectToken, property)
    }

    override fun postFetchId(projectToken: String, id: ExponeaFetchId): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_ID, projectToken, id)
    }

    override fun postFetchSegmentation(projectToken: String, id: ExponeaFetchId): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_SEGMENTATION, projectToken, id)
    }

    override fun postFetchExpression(projectToken: String, id: ExponeaFetchId): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_EXPRESSION, projectToken, id)
    }

    override fun postFetchPrediction(projectToken: String, id: ExponeaFetchId): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_PREDICTION, projectToken, id)
    }

    override fun postFetchRecommendation(
        projectToken: String,
        recommendation: CustomerRecommendation
    ): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_RECOMMENDATION, projectToken,
                recommendation.toHashMap())
    }

    override fun postFetchAttributes(projectToken: String, attributes: CustomerAttributes): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES, projectToken,
                attributes.toHashMap())
    }

    override fun postFetchEvents(projectToken: String, events: FetchEventsRequest): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_EVENTS, projectToken, events.toHashMap())
    }

    override fun postFetchAllProperties(projectToken: String, customerIds: CustomerIds): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_EXPORT_ALL_PROPERTIES,
                projectToken, customerIds.toHashMap())
    }

    override fun postFetchAllCustomers(projectToken: String, customer: CustomerExportModel): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_EXPORT_ALL, projectToken, customer)
    }

    override fun postAnonymize(projectToken: String, customerIds: CustomerIds): Call {
        return doPost(ApiEndPoint.EndPointName.CUSTOMERS_ANONYMIZE, projectToken,
                customerIds.toHashMap())
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

    private fun doPost(endPointName: ApiEndPoint.EndPointName, projectToken: String, bodyContent: Any?): Call {
        val endpoint = ApiEndPoint(endPointName, projectToken).toString()
        val jsonBody = bodyContent?.let { gson.toJson(it) }
        return networkManager.post(endpoint, jsonBody)
    }
}
