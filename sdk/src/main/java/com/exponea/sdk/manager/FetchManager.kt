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
            customerEvents: CustomerEvents,
            onSuccess: (Result<ArrayList<CustomerEventModel>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    )

}