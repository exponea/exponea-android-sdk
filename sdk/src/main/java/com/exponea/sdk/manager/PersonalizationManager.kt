package com.exponea.sdk.manager

import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.PersonalizationData
import com.exponea.sdk.models.Result

interface PersonalizationManager {
    fun showBanner(projectToken: String, customerIds: CustomerIds)
    fun getBannersConfiguration(projectToken: String,
                                customerIds: CustomerIds,
                                onSuccess: (Result<ArrayList<PersonalizationData>>) -> Unit,
                                onFailure: (String) -> Unit)
    fun getPersonalization(projectToken: String, customerIds: CustomerIds)
}