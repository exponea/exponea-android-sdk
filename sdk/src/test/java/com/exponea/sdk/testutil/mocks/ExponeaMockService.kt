package com.exponea.sdk.testutil.mocks

import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaProject
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

    override fun postCampaignClick(exponeaProject: ExponeaProject, event: Event): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postEvent(exponeaProject: ExponeaProject, event: Event): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postCustomer(exponeaProject: ExponeaProject, event: Event): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchConsents(exponeaProject: ExponeaProject): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchAttributes(
        exponeaProject: ExponeaProject,
        attributesRequest: CustomerAttributesRequest
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchInAppMessages(exponeaProject: ExponeaProject, customerIds: CustomerIds): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    private fun mockFailCall(): Call {
        val mockInterceptor = MockInterceptor().apply {
            addRule()
                .get().or().post().or().put()
                .url(dummyUrl)
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
                .respond(HTTP_200_OK, response)
        }
        val okHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(mockInterceptor)
            .build()

        return okHttpClient.newCall(Request.Builder().url(dummyUrl).get().build())
    }

    override fun postPushSelfCheck(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        pushToken: String,
        tokenType: TokenType
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postFetchAppInbox(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        syncToken: String?
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }

    override fun postReadFlagAppInbox(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        messageIds: List<String>,
        syncToken: String
    ): Call {
        return if (success) mockSuccessCall() else mockFailCall()
    }
}
