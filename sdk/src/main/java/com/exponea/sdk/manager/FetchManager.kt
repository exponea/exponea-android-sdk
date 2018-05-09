package com.exponea.sdk.manager

import com.exponea.sdk.models.CustomerAttributeModel
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.CustomerEvents
import com.exponea.sdk.models.Result

interface FetchManager {

    fun fetchCustomerAttributes(
            projectToken: String,
            attributes: CustomerAttributes,
            onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
            onFailure: (String) -> Unit
    )

    fun fetchCustomerEvents(
            projectToken: String,
            customerEvents: CustomerEvents,
            onSuccess: () -> Unit,
            onFailure: (String) -> Unit
    )
}