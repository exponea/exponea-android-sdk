package com.exponea.sdk.testutil.mocks

import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.RecommendationsRequest
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.util.TokenType
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.mock.HttpCode.HTTP_200_OK
import okhttp3.mock.HttpCode.HTTP_400_BAD_REQUEST
import okhttp3.mock.MockInterceptor

internal class ExponeaMockService(
    private val success: Boolean,
    private val response: ResponseBody? = null
) : ExponeaService {

    private val server = ExponeaMockServer.createServer()
    private val dummyUrl = server.url("/").toString()

    override fun postCampaignClick(
        integrationConfig: IntegrationConfig,
        event: Event,
        isCurrentCustomer: Boolean
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postEvent(integrationConfig: IntegrationConfig, event: Event, isCurrentCustomer: Boolean): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postCustomer(integrationConfig: IntegrationConfig, event: Event, isCurrentCustomer: Boolean): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun fetchConsents(integrationConfig: IntegrationConfig): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchAttributes(
        projectConfig: ProjectConfig,
        attributesRequest: CustomerAttributesRequest
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchRecommendations(
        streamConfig: StreamConfig,
        recommendationsRequest: RecommendationsRequest
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchInAppMessages(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        ifNoneMatch: String?
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    private fun mockFailCall(): Call {
        val mockInterceptor = MockInterceptor().apply {
            addRule()
                .get().or().post().or().put()
                .url(dummyUrl)
                .anyTimes()
                .respond(HTTP_400_BAD_REQUEST, response)
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
                .anyTimes()
                .respond(HTTP_200_OK, response)
        }
        val okHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(mockInterceptor)
            .build()

        return okHttpClient.newCall(Request.Builder().url(dummyUrl).get().build())
    }

    override fun postPushSelfCheck(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        pushToken: String,
        tokenType: TokenType,
        applicationId: String
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun fetchStaticInAppContentBlocks(integrationConfig: IntegrationConfig): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun fetchPersonalizedInAppContentBlocks(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        contentBlockIds: List<String>,
        ifNoneMatch: String?
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        syncToken: String?,
        applicationId: String
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postReadFlagAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        messageIds: List<String>,
        syncToken: String
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun fetchSegments(integrationConfig: IntegrationConfig, engagementCookieId: String): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun linkIdsToCookie(
        integrationConfig: IntegrationConfig,
        engagementCookieId: String,
        externalIds: HashMap<String, String?>
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }
}
