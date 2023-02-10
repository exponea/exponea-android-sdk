package com.exponea.sdk.network

import com.exponea.sdk.models.ApiEndPoint
import com.exponea.sdk.models.CampaignClickEvent
import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.util.TokenType
import com.google.gson.Gson
import okhttp3.Call

internal class ExponeaServiceImpl(
    private val gson: Gson,
    private val networkManager: NetworkHandler
) : ExponeaService {

    override fun postCampaignClick(exponeaProject: ExponeaProject, event: Event): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.TRACK_CAMPAIGN, CampaignClickEvent(event))
    }

    override fun postEvent(exponeaProject: ExponeaProject, event: Event): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.TRACK_EVENTS, event)
    }

    override fun postCustomer(exponeaProject: ExponeaProject, event: Event): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.TRACK_CUSTOMERS, event)
    }

    override fun postFetchAttributes(
        exponeaProject: ExponeaProject,
        attributesRequest: CustomerAttributesRequest
    ): Call {
        return doPost(exponeaProject, ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES, attributesRequest)
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

    override fun postFetchAppInbox(exponeaProject: ExponeaProject, customerIds: CustomerIds, syncToken: String?): Call {
        val reqBody = hashMapOf<String, Any>(
            "customer_ids" to customerIds.toHashMap()
        )
        if (syncToken != null) {
            reqBody.put("sync_token", syncToken)
        }
        return doPost(
            exponeaProject,
            ApiEndPoint.EndPointName.MESSAGE_INBOX,
            reqBody
        )
    }

    override fun postReadFlagAppInbox(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        messageIds: List<String>,
        syncToken: String
    ): Call {
        val reqBody = hashMapOf(
            "customer_ids" to customerIds.toHashMap(),
            "message_ids" to messageIds,
            "sync_token" to syncToken
        )
        return doPost(
            exponeaProject,
            ApiEndPoint.EndPointName.MESSAGE_INBOX_READ,
            reqBody
        )
    }

    override fun postPushSelfCheck(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        pushToken: String,
        tokenType: TokenType
    ): Call {
        return doPost(
            exponeaProject,
            ApiEndPoint.EndPointName.PUSH_SELF_CHECK,
            hashMapOf(
                "platform" to tokenType.selfCheckProperty,
                "customer_ids" to customerIds.toHashMap(),
                "push_notification_id" to pushToken
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
