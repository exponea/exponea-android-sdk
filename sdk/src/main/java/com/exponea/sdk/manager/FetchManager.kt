package com.exponea.sdk.manager

import com.exponea.sdk.models.Banner
import com.exponea.sdk.models.BannerResult
import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.Personalization
import com.exponea.sdk.models.Result

internal interface FetchManager {

    fun fetchBannerConfiguration(
        projectToken: String,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchBanner(
        projectToken: String,
        bannerConfig: Banner,
        onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchConsents(
        projectToken: String,
        onSuccess: (Result<ArrayList<Consent>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun fetchRecommendation(
        projectToken: String,
        recommendationRequest: CustomerRecommendationRequest,
        onSuccess: (Result<ArrayList<CustomerRecommendation>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )
}
