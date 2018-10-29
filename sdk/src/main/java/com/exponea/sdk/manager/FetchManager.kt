package com.exponea.sdk.manager

import com.exponea.sdk.models.*

interface FetchManager {

    @Deprecated("Basic authorization was deprecated and fetching data will not be available in the future.")
    fun fetchCustomerAttributes(
            projectToken: String,
            attributes: CustomerAttributes,
            onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    )

    @Deprecated("Basic authorization was deprecated and fetching data will not be available in the future.")
    fun fetchCustomerEvents(
            projectToken: String,
            customerEvents: FetchEventsRequest,
            onSuccess: (Result<ArrayList<CustomerEvent>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    )

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

}