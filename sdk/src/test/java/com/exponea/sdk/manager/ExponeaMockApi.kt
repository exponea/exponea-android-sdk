package com.exponea.sdk.manager

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.*
import kotlinx.coroutines.experimental.delay
import java.util.*

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

    suspend fun flush() {
        Exponea.flush()
        delay(2000L)
    }

}