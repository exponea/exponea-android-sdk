package com.exponea.sdk.network

import com.exponea.sdk.models.*
import okhttp3.Call
import okhttp3.Response
import java.io.IOException

interface ExponeaService {
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
    fun postFetchEvents(projectToken: String, events: CustomerEvents): Call
    fun postFetchAllProperties(projectToken: String, customerIds: CustomerIds): Call
    fun postFetchAllCustomers(projectToken: String, customer: CustomerExportModel): Call
    fun postAnonymize(projectToken: String, customerIds: CustomerIds): Call
}