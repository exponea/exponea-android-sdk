package com.exponea.sdk.manager

import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.Result

internal interface FetchManager {
    fun fetchConsents(
        exponeaProject: ExponeaProject,
        onSuccess: (Result<ArrayList<Consent>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchRecommendation(
        exponeaProject: ExponeaProject,
        recommendationRequest: CustomerRecommendationRequest,
        onSuccess: (Result<ArrayList<CustomerRecommendation>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchInAppMessages(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<InAppMessage>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchAppInbox(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        syncToken: String?,
        onSuccess: (Result<ArrayList<MessageItem>?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun markAppInboxAsRead(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        syncToken: String,
        messageIds: List<String>,
        onSuccess: (Result<Any?>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )
}
