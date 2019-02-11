package com.exponea.sdk.manager

interface IapManager {
    fun configure(skuList: List<String>)
    fun startObservingPayments()
    fun stopObservingPayments()
    fun trackPurchase(properties: HashMap<String, Any>)
    fun getAvailableProducts(skuList: List<String>)
}