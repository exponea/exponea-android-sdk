package com.exponea.sdk.manager

import com.exponea.sdk.models.*

interface FetchManager {

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
}