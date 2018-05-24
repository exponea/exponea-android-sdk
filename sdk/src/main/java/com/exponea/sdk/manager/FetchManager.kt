package com.exponea.sdk.manager

import com.exponea.sdk.models.*

interface FetchManager {

    fun fetchCustomerAttributes(
            projectToken: String,
            attributes: CustomerAttributes,
            onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    )

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