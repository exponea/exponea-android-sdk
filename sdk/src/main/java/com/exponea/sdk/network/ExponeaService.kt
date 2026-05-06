package com.exponea.sdk.network
import com.exponea.sdk.models.CustomerAttributesRequest
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.RecommendationsRequest
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.util.TokenType
import okhttp3.Call

internal interface ExponeaService {
    fun postEvent(
        integrationConfig: IntegrationConfig,
        event: Event,
        isCurrentCustomer: Boolean
    ): Call
    fun postCustomer(integrationConfig: IntegrationConfig, event: Event, isCurrentCustomer: Boolean): Call
    fun postFetchAttributes(projectConfig: ProjectConfig, attributesRequest: CustomerAttributesRequest): Call
    fun postFetchRecommendations(streamConfig: StreamConfig, recommendationsRequest: RecommendationsRequest): Call
    fun fetchConsents(integrationConfig: IntegrationConfig): Call
    fun postCampaignClick(integrationConfig: IntegrationConfig, event: Event, isCurrentCustomer: Boolean): Call
    fun postFetchInAppMessages(integrationConfig: IntegrationConfig, customerIds: CustomerIds): Call
    fun postFetchAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        syncToken: String?,
        applicationId: String
    ): Call
    fun postReadFlagAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        messageIds: List<String>,
        syncToken: String
    ): Call
    fun postPushSelfCheck(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        pushToken: String,
        tokenType: TokenType,
        applicationId: String
    ): Call
    fun fetchStaticInAppContentBlocks(integrationConfig: IntegrationConfig): Call
    fun fetchPersonalizedInAppContentBlocks(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        contentBlockIds: List<String>
    ): Call

    fun fetchSegments(
        integrationConfig: IntegrationConfig,
        engagementCookieId: String
    ): Call

    fun linkIdsToCookie(
        integrationConfig: IntegrationConfig,
        engagementCookieId: String,
        externalIds: HashMap<String, String?>
    ): Call
}
