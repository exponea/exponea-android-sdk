package com.exponea.sdk.manager

import com.exponea.sdk.models.PropertiesList

interface IapManager {
    fun configure()
    fun startObservingPayments()
    fun stopObservingPayments()
    fun trackPurchase(properties: HashMap<String, Any>)
    fun getAvailableProducts()
}