package com.exponea.sdk.network

import com.exponea.sdk.models.ApiEndPoint
import com.exponea.sdk.models.ApiEndPoint.Companion.COOKIE_ID_PLACEHOLDER
import com.exponea.sdk.models.ApiEndPoint.Companion.STREAM_ID_PLACEHOLDER
import com.exponea.sdk.models.ApiEndPoint.Companion.TOKEN_PLACEHOLDER
import com.exponea.sdk.models.ApiEndPoint.EndPointName.CONSENTS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.CUSTOMERS_ATTRIBUTES
import com.exponea.sdk.models.ApiEndPoint.EndPointName.INAPP_CONTENT_BLOCKS_PERSONAL
import com.exponea.sdk.models.ApiEndPoint.EndPointName.INAPP_CONTENT_BLOCKS_STATIC
import com.exponea.sdk.models.ApiEndPoint.EndPointName.IN_APP_MESSAGES
import com.exponea.sdk.models.ApiEndPoint.EndPointName.LINK_CUSTOMER_IDS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.MESSAGE_INBOX
import com.exponea.sdk.models.ApiEndPoint.EndPointName.MESSAGE_INBOX_READ
import com.exponea.sdk.models.ApiEndPoint.EndPointName.PUSH_SELF_CHECK
import com.exponea.sdk.models.ApiEndPoint.EndPointName.SEGMENTS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.TRACK_CAMPAIGN
import com.exponea.sdk.models.ApiEndPoint.EndPointName.TRACK_CUSTOMERS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.TRACK_EVENTS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_CONSENTS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_INAPP_CONTENT_BLOCKS_PERSONAL
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_INAPP_CONTENT_BLOCKS_STATIC
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_IN_APP_MESSAGES
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_LINK_CUSTOMER_IDS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_MESSAGE_INBOX
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_MESSAGE_INBOX_READ
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_PUSH_SELF_CHECK
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_RECOMMENDATIONS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_SEGMENTS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_TRACK_CAMPAIGN
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_TRACK_CUSTOMERS
import com.exponea.sdk.models.ApiEndPoint.EndPointName.U_TRACK_EVENTS
import com.exponea.sdk.models.CampaignClickEvent
import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.RecommendationsRequest
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.network.auth.AuthMode
import com.exponea.sdk.network.auth.AuthStrategy
import com.exponea.sdk.util.TokenType
import com.google.gson.Gson
import okhttp3.Call

internal class ExponeaServiceImpl(
    private val gson: Gson,
    private val networkManager: NetworkHandler
) : ExponeaService {

    override fun postCampaignClick(
        integrationConfig: IntegrationConfig,
        event: Event,
        isCurrentCustomer: Boolean
    ) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(TRACK_CAMPAIGN).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig -> ApiEndPoint(U_TRACK_CAMPAIGN).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> if (isCurrentCustomer) AuthStrategy.Token() else AuthStrategy.None
        },
        CampaignClickEvent(event)
    )

    override fun postEvent(integrationConfig: IntegrationConfig, event: Event, isCurrentCustomer: Boolean) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(TRACK_EVENTS).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig -> ApiEndPoint(U_TRACK_EVENTS).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> if (isCurrentCustomer) AuthStrategy.Token() else AuthStrategy.None
        },
        event
    )

    override fun postCustomer(integrationConfig: IntegrationConfig, event: Event, isCurrentCustomer: Boolean) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(TRACK_CUSTOMERS).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig -> ApiEndPoint(U_TRACK_CUSTOMERS).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> if (isCurrentCustomer) AuthStrategy.Token() else AuthStrategy.None
        },
        event
    )

    override fun postFetchAttributes(
        projectConfig: ProjectConfig,
        attributesRequest: CustomerAttributesRequest
    ) = doPost(
        projectConfig,
        ApiEndPoint(CUSTOMERS_ATTRIBUTES).applyProjectToken(projectConfig.projectToken),
        AuthStrategy.PublicApiKey(projectConfig.authorization),
        attributesRequest
    )

    override fun postFetchRecommendations(
        streamConfig: StreamConfig,
        recommendationsRequest: RecommendationsRequest
    ) = doPost(
        streamConfig,
        ApiEndPoint(U_RECOMMENDATIONS).applyStreamId(streamConfig.streamId),
        AuthStrategy.Token(),
        recommendationsRequest
    )

    override fun fetchConsents(integrationConfig: IntegrationConfig) = doGet(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(CONSENTS).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig -> ApiEndPoint(U_CONSENTS).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> AuthStrategy.Token()
        }
    )

    override fun postFetchInAppMessages(integrationConfig: IntegrationConfig, customerIds: CustomerIds) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(IN_APP_MESSAGES).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig -> ApiEndPoint(U_IN_APP_MESSAGES).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> AuthStrategy.Token()
        },
        hashMapOf(
            "customer_ids" to customerIds.toHashMap(),
            "device" to "android"
        )
    )

    override fun postFetchAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        syncToken: String?,
        applicationId: String
    ): Call {
        val reqBody = hashMapOf<String, Any>(
            "customer_ids" to customerIds.toHashMap(),
            "application_id" to applicationId
        )
        if (syncToken != null) {
            reqBody["sync_token"] = syncToken
        }
        return doPost(
            integrationConfig,
            when (integrationConfig) {
                is ProjectConfig -> ApiEndPoint(MESSAGE_INBOX).applyProjectToken(integrationConfig.projectToken)
                is StreamConfig -> ApiEndPoint(U_MESSAGE_INBOX).applyStreamId(integrationConfig.streamId)
            },
            when (integrationConfig) {
                is ProjectConfig -> AuthStrategy.LegacyToken(integrationConfig.authorization, AuthMode.REQUIRED)
                is StreamConfig -> AuthStrategy.Token(mode = AuthMode.REQUIRED)
            },
            reqBody
        )
    }

    override fun postReadFlagAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        messageIds: List<String>,
        syncToken: String
    ) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(MESSAGE_INBOX_READ).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig -> ApiEndPoint(U_MESSAGE_INBOX_READ).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.LegacyToken(integrationConfig.authorization, AuthMode.REQUIRED)
            is StreamConfig -> AuthStrategy.Token(mode = AuthMode.REQUIRED)
        },
        hashMapOf(
            "customer_ids" to customerIds.toHashMap(),
            "message_ids" to messageIds,
            "sync_token" to syncToken
        )
    )

    override fun postPushSelfCheck(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        pushToken: String,
        tokenType: TokenType
    ) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(PUSH_SELF_CHECK).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig -> ApiEndPoint(U_PUSH_SELF_CHECK).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> AuthStrategy.Token()
        },
        hashMapOf(
            "platform" to tokenType.selfCheckProperty,
            "customer_ids" to customerIds.toHashMap(),
            "push_notification_id" to pushToken
        )
    )

    override fun fetchStaticInAppContentBlocks(integrationConfig: IntegrationConfig) = doGet(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig ->
                ApiEndPoint(INAPP_CONTENT_BLOCKS_STATIC).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig ->
                ApiEndPoint(U_INAPP_CONTENT_BLOCKS_STATIC).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.LegacyToken(integrationConfig.authorization)
            is StreamConfig -> AuthStrategy.Token()
        }
    )

    override fun fetchPersonalizedInAppContentBlocks(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        contentBlockIds: List<String>
    ) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig ->
                ApiEndPoint(INAPP_CONTENT_BLOCKS_PERSONAL).applyProjectToken(integrationConfig.projectToken)
            is StreamConfig ->
                ApiEndPoint(U_INAPP_CONTENT_BLOCKS_PERSONAL).applyStreamId(integrationConfig.streamId)
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.LegacyToken(integrationConfig.authorization)
            is StreamConfig -> AuthStrategy.Token()
        },
        hashMapOf(
            "customer_ids" to customerIds.toHashMap(),
            "content_block_ids" to contentBlockIds
        )
    )

    override fun fetchSegments(
        integrationConfig: IntegrationConfig,
        engagementCookieId: String
    ) = doGet(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(SEGMENTS).applyPlaceholders(
                mapOf(
                    TOKEN_PLACEHOLDER to integrationConfig.projectToken,
                    COOKIE_ID_PLACEHOLDER to engagementCookieId
                )
            )
            is StreamConfig -> ApiEndPoint(U_SEGMENTS).applyPlaceholders(
                mapOf(
                    STREAM_ID_PLACEHOLDER to integrationConfig.streamId,
                    COOKIE_ID_PLACEHOLDER to engagementCookieId
                )
            )
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> AuthStrategy.Token()
        }
    )

    override fun linkIdsToCookie(
        integrationConfig: IntegrationConfig,
        engagementCookieId: String,
        externalIds: HashMap<String, String?>
    ) = doPost(
        integrationConfig,
        when (integrationConfig) {
            is ProjectConfig -> ApiEndPoint(LINK_CUSTOMER_IDS).applyPlaceholders(
                mapOf(
                    TOKEN_PLACEHOLDER to integrationConfig.projectToken,
                    COOKIE_ID_PLACEHOLDER to engagementCookieId
                )
            )
            is StreamConfig -> ApiEndPoint(U_LINK_CUSTOMER_IDS).applyPlaceholders(
                mapOf(
                    STREAM_ID_PLACEHOLDER to integrationConfig.streamId,
                    COOKIE_ID_PLACEHOLDER to engagementCookieId
                )
            )
        },
        when (integrationConfig) {
            is ProjectConfig -> AuthStrategy.PublicApiKey(integrationConfig.authorization)
            is StreamConfig -> AuthStrategy.Token()
        },
        hashMapOf(
            "external_ids" to externalIds
        )
    )

    internal fun doPost(
        integrationConfig: IntegrationConfig,
        endpoint: String,
        authStrategy: AuthStrategy,
        bodyContent: Any
    ) = networkManager.post(
        integrationConfig.baseUrl + endpoint,
        authStrategy,
        bodyContent.let { gson.toJson(it) }
    )

    internal fun doGet(
        integrationConfig: IntegrationConfig,
        endpoint: String,
        authStrategy: AuthStrategy
    ) = networkManager.get(
        integrationConfig.baseUrl + endpoint,
        authStrategy
    )
}
