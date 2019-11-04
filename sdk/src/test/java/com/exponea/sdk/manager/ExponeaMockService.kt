package com.exponea.sdk.manager

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.CustomerExportModel
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.ExponeaFetchId
import com.exponea.sdk.models.ExponeaFetchProperty
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.network.ExponeaService
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mock.HttpCodes.HTTP_200_OK
import okhttp3.mock.HttpCodes.HTTP_400_BAD_REQUEST
import okhttp3.mock.MockInterceptor
import okhttp3.mockwebserver.MockWebServer

internal class ExponeaMockService(private val success: Boolean) : ExponeaService {

    private val server = MockWebServer()
    private val dummyUrl = server.url("/").toString()

    override fun postCampaignClick(projectToken: String, event: ExportedEventType): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postEvent(projectToken: String, event: ExportedEventType): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postCustomer(projectToken: String, event: ExportedEventType): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postRotateToken(projectToken: String): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchConsents(projectToken: String): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postRevokeToken(projectToken: String): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchProperty(projectToken: String, property: ExponeaFetchProperty): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchId(projectToken: String, id: ExponeaFetchId): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchSegmentation(projectToken: String, id: ExponeaFetchId): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchExpression(projectToken: String, id: ExponeaFetchId): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchPrediction(projectToken: String, id: ExponeaFetchId): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchRecommendation(
        projectToken: String,
        recommendation: CustomerRecommendation
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchAttributes(projectToken: String, attributes: CustomerAttributes): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchAllProperties(projectToken: String, customerIds: CustomerIds): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchAllCustomers(projectToken: String, customer: CustomerExportModel): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postAnonymize(projectToken: String, customerIds: CustomerIds): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun getBannerConfiguration(projectToken: String): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchBanner(projectToken: String, banner: Banner): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    private fun mockFailCall(): Call {
        val mockInterceptor = MockInterceptor().apply {
            addRule()
                .get().or().post().or().put()
                .url(dummyUrl)
                .respond(HTTP_400_BAD_REQUEST)
        }
        val okHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(mockInterceptor)
            .build()

        return okHttpClient.newCall(Request.Builder().url(dummyUrl).get().build())
    }

    private fun mockSuccessCall(): Call {
        val mockInterceptor = MockInterceptor().apply {
            addRule()
                .get().or().post().or().put()
                .url(dummyUrl)
                .respond(HTTP_200_OK)
        }
        val okHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(mockInterceptor)
            .build()

        return okHttpClient.newCall(Request.Builder().url(dummyUrl).get().build())
    }
}
