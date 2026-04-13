package com.exponea.sdk.manager

import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPersonalizedData
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.SegmentationCategories

internal interface FetchManager {
    fun fetchConsents(
        integrationConfig: IntegrationConfig,
        onSuccess: (Result<ArrayList<Consent>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchRecommendation(
        integrationConfig: IntegrationConfig,
        customerIds: Map<String, Any?>,
        options: CustomerRecommendationOptions,
        onSuccess: (Result<ArrayList<CustomerRecommendation>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchInAppMessages(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<InAppMessage>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchAppInbox(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        syncToken: String?,
        applicationId: String,
        onSuccess: (Result<ArrayList<MessageItem>?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun markAppInboxAsRead(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        syncToken: String,
        messageIds: List<String>,
        onSuccess: (Result<Any?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchStaticInAppContentBlocks(
        integrationConfig: IntegrationConfig,
        onSuccess: (Result<ArrayList<InAppContentBlock>?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchPersonalizedContentBlocks(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        contentBlockIds: List<String>,
        onSuccess: (Result<ArrayList<InAppContentBlockPersonalizedData>?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchSegments(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds,
        onSuccess: (Result<SegmentationCategories>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun linkCustomerIdsSync(
        integrationConfig: IntegrationConfig,
        customerIds: CustomerIds
    ): Result<out Any?>
}
