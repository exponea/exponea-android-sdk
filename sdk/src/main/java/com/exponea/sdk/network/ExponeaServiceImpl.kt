package com.exponea.sdk.network

import com.exponea.sdk.models.*
import com.google.gson.Gson
import okhttp3.Call

class ExponeaServiceImpl(
        private val gson: Gson,
        private val networkManager: NetworkHandler
) : ExponeaService {

    override fun postEvent(projectToken: String, event: ExportedEventType): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TRACK_EVENTS, projectToken).toString()
        val jsonBody = gson.toJson(event)
        return networkManager.post(endpoint, jsonBody)
    }

    override fun postCustomer(projectToken: String, event: ExportedEventType): Call {
        val endpoint = ApiEndPoint(
                ApiEndPoint.EndPointName.TRACK_CUSTOMERS,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(event)
        return networkManager.post(endpoint, jsonBody)
    }

    override fun postRotateToken(projectToken: String): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TOKEN_ROTATE, projectToken).toString()
        return networkManager.post(endpoint, null)
    }

    override fun postRevokeToken(projectToken: String): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TOKEN_REVOKE, projectToken).toString()
        return networkManager.post(endpoint, null)
    }

    override fun postFetchProperty(projectToken: String, property: ExponeaFetchProperty): Call {
        val endpoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_PROPERTY,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(property)
        return networkManager.post(endpoint, jsonBody)
    }

    override fun postFetchId(projectToken: String, id: ExponeaFetchId): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_ID, projectToken).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endpoint, jsonBody)
    }

    override fun postFetchSegmentation(projectToken: String, id: ExponeaFetchId): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_SEGMENTATION,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postFetchExpression(projectToken: String, id: ExponeaFetchId): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_EXPRESSION,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postFetchPrediction(projectToken: String, id: ExponeaFetchId): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_PREDICTION,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postFetchRecommendation(
            projectToken: String,
            recommendation: CustomerRecommendation
    ): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_RECOMMENDATION,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(recommendation.toHashMap())
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postFetchAttributes(projectToken: String, attributes: CustomerAttributes): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(attributes.toHashMap())
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postFetchEvents(projectToken: String, events: FetchEventsRequest): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_EVENTS,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(events.toHashMap())
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postFetchAllProperties(projectToken: String, customerIds: CustomerIds): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_EXPORT_ALL_PROPERTIES,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(customerIds.toHashMap())
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postFetchAllCustomers(projectToken: String, customer: CustomerExportModel): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_EXPORT_ALL,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(customer)
        return networkManager.post(endPoint, jsonBody)
    }

    override fun postAnonymize(projectToken: String, customerIds: CustomerIds): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CUSTOMERS_ANONYMIZE,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(customerIds.toHashMap())
        return networkManager.post(endPoint, jsonBody)
    }

    override fun getBannerConfiguration(projectToken: String): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.CONFIGURE_BANNER,
                projectToken
        ).toString()
        return networkManager.get(endPoint, null)
    }

    override fun postFetchBanner(projectToken: String, banner: Banner): Call {
        val endPoint = ApiEndPoint(
                ApiEndPoint.EndPointName.SHOW_BANNER,
                projectToken
        ).toString()
        val jsonBody = gson.toJson(banner)
        return networkManager.post(endPoint, jsonBody)
    }
}