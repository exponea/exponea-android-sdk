package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerAttributeModel
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.Result
import kotlinx.coroutines.experimental.delay

object ExponeaMockApi {

    suspend fun fetchCustomerId(attributes: CustomerAttributes,
                                onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
                                onFailure: (Result<FetchError>) -> Unit) {
        Exponea.fetchCustomerAttributes(
                customerAttributes = attributes,
                onSuccess = onSuccess,
                onFailure = onFailure
        )
        delay(2000L)
    }

    suspend fun fetchCustomerAttributes(attributes: CustomerAttributes,
                                        onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
                                        onFailure: (Result<FetchError>) -> Unit) {
        Exponea.fetchCustomerAttributes(
                customerAttributes = attributes,
                onSuccess = onSuccess,
                onFailure = onFailure
        )
        delay(2000L)
    }

}