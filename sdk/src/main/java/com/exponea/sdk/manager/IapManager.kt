package com.exponea.sdk.manager

interface IapManager {
    fun configure()
    fun startObservingPayments()
    fun stopObservingPayments()
    fun trackPurchase(properties: HashMap<String, Any>)
    fun getAvailableProducts()
}