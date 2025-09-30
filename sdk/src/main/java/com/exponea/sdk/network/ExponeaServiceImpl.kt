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

    override fun postFetchAppInbox(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        syncToken: String?,
        applicationId: String
    ): Call {
        val reqBody = hashMapOf<String, Any>(
            "customer_ids" to customerIds.toHashMap(),
            "application_id" to applicationId
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

    internal fun doPost(
        exponeaProject: ExponeaProject,
        endpointTemplate: ApiEndPoint.EndPointName,
        bodyContent: Any?
    ): Call {
        return doPost(
            exponeaProject,
            ApiEndPoint.forName(endpointTemplate).withToken(exponeaProject.projectToken).toString(),
            bodyContent
        )
    }

    internal fun doPost(
        exponeaProject: ExponeaProject,
        endpoint: String,
        bodyContent: Any?
    ): Call {
        val jsonBody = bodyContent?.let { gson.toJson(it) }
        return networkManager.post(exponeaProject.baseUrl + endpoint, exponeaProject.authorization, jsonBody)
    }

    override fun fetchStaticInAppContentBlocks(exponeaProject: ExponeaProject): Call {
        return doPost(
            exponeaProject,
            ApiEndPoint.EndPointName.INAPP_CONTENT_BLOCKS_STATIC,
            null
        )
    }

    override fun fetchPersonalizedInAppContentBlocks(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        contentBlockIds: List<String>
    ): Call {
        val reqBody = hashMapOf(
            "customer_ids" to customerIds.toHashMap(),
            "content_block_ids" to contentBlockIds
        )
        return doPost(
            exponeaProject,
            ApiEndPoint.EndPointName.INAPP_CONTENT_BLOCKS_PERSONAL,
            reqBody
        )
    }

    override fun fetchSegments(
        exponeaProject: ExponeaProject,
        engagementCookieId: String
    ): Call {
        return doPost(
            exponeaProject,
            ApiEndPoint.forName(ApiEndPoint.EndPointName.SEGMENTS)
                .withToken(exponeaProject.projectToken)
                .withQueryParam("cookie", engagementCookieId)
                .toString(),
            null
        )
    }

    override fun linkIdsToCookie(
        exponeaProject: ExponeaProject,
        engagementCookieId: String,
        externalIds: HashMap<String, String?>
    ): Call {
        val reqBody = hashMapOf(
            "external_ids" to externalIds
        )
        return doPost(
            exponeaProject,
            ApiEndPoint.forName(ApiEndPoint.EndPointName.LINK_CUSTOMER_IDS)
                .withToken(exponeaProject.projectToken)
                .withPathParam(ApiEndPoint.COOKIE_ID_PATH_PARAM, engagementCookieId)
                .toString(),
            reqBody
        )
    }
}
