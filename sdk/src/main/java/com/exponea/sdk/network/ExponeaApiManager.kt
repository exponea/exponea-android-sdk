package com.exponea.sdk.network

import com.exponea.sdk.models.*
import com.google.gson.Gson
import okhttp3.Call

class ExponeaApiManager(
        private val gson: Gson,
        private val networkManager: NetworkManager
) {
    fun postEvent(projectToken: String, event: ExportedEventType): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TRACK_EVENTS, projectToken).toString()
        val jsonBody = gson.toJson(event)
        return networkManager.post(endpoint, jsonBody)
    }
    fun postCustomer(projectToken: String, event: ExportedEventType): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TRACK_CUSTOMERS, projectToken).toString()
        val jsonBody = gson.toJson(event)
        return networkManager.post(endpoint, jsonBody)
    }
    fun postRotateToken(projectToken: String): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TOKEN_ROTATE, projectToken).toString()
        return networkManager.post(endpoint, null)
    }
    fun postRevokeToken(projectToken: String): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TOKEN_REVOKE, projectToken).toString()
        return networkManager.post(endpoint, null)
    }
    fun postFetchProperty(projectToken: String, property: ExponeaFetchProperty): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_PROPERTY, projectToken).toString()
        val jsonBody = gson.toJson(property)
        return networkManager.post(endpoint, jsonBody)
    }
    fun postFetchId(projectToken: String, id: ExponeaFetchId): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_ID, projectToken).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endpoint, jsonBody)
    }
    fun postFetchSegmentation(projectToken: String, id: ExponeaFetchId): Call {
        val endPoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_SEGMENTATION, projectToken).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endPoint, jsonBody)
    }
    fun postFetchExpression(projectToken: String, id: ExponeaFetchId): Call {
        val endPoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_EXPRESSION, projectToken).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endPoint, id)
    }
    fun postFetchPrediction(projectToken: String, id: ExponeaFetchId): Call {
        val endPoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_PREDICTION, projectToken).toString()
        val jsonBody = gson.toJson(id)
        return networkManager.post(endPoint, id)
    }
    fun postFetchRecommendation(projectToken: String, recommendation: CustomerRecommendation): Call {
        val endPoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_RECOMMENDATION, projectToken).toString()
        val jsonBody = gson.toJson(recommendation)
        return networkManager.post(endPoint, jsonBody)
    }
    fun postFetchAttributes(projectToken: String, attributes: CustomerAttributes): Call {
        val endPoint = ApiEndPoint(ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES, projectToken).toString()
        val jsonBody = gson.toJson(attributes)
        return networkManager.post(endPoint, jsonBody)
    }
}
