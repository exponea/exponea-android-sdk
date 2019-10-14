package com.exponea.sdk.manager

import com.exponea.sdk.models.*

interface PersonalizationManager {
    fun showBanner(projectToken: String, customerIds: CustomerIds)

    fun getBannersConfiguration(
            projectToken: String,
            customerIds: CustomerIds,
            onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    )

    fun getPersonalization(projectToken: String, customerIds: CustomerIds)

    fun getWebLayer(
            projectToken: String,
            customerIds: CustomerIds,
            onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    )
}