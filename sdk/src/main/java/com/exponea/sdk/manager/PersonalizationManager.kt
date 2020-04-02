package com.exponea.sdk.manager

import com.exponea.sdk.models.BannerResult
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.Personalization
import com.exponea.sdk.models.Result

internal interface PersonalizationManager {
    fun showBanner(exponeaProject: ExponeaProject, customerIds: CustomerIds)

    fun getBannersConfiguration(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<Personalization>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )

    fun getPersonalization(exponeaProject: ExponeaProject, customerIds: CustomerIds)

    fun getWebLayer(
        exponeaProject: ExponeaProject,
        customerIds: CustomerIds,
        onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    )
}
