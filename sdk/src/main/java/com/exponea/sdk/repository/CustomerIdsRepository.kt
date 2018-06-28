package com.exponea.sdk.repository

import com.exponea.sdk.models.CustomerIds

interface CustomerIdsRepository {

    fun get() : CustomerIds

    fun set(customerIds: CustomerIds)
}