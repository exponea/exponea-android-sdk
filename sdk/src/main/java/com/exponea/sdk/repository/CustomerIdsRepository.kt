package com.exponea.sdk.repository

import com.exponea.sdk.models.CustomerIds

internal interface CustomerIdsRepository {

    fun get() : CustomerIds

    fun set(customerIds: CustomerIds)

    fun clear()
}
