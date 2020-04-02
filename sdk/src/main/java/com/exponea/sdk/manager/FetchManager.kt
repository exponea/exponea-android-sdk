package com.exponea.sdk.manager

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.BannerResult
import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.Personalization
import com.exponea.sdk.models.Result

internal interface FetchManager {

    fun fetchBannerConfiguration(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchBanner(
        exponeaProject: ExponeaProject,
        bannerConfig: Banner,
        onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

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
}
