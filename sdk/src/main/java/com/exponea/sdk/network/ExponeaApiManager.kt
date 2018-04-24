package com.exponea.sdk.network

import com.exponea.sdk.models.ApiEndPoint
import com.exponea.sdk.models.ExportedEventType
import com.google.gson.Gson
import okhttp3.Call

class ExponeaApiManager(
        private val gson: Gson,
        private val networkManager: NetworkManager
) {
    fun postEvent(projectToken: String, event: ExportedEventType): Call {
        val endpoint = ApiEndPoint(ApiEndPoint.EndPointName.TRACK_CUSTOMERS, projectToken).toString()
        val jsonBody = gson.toJson(event)
        return networkManager.post(endpoint, jsonBody)
    }
}
