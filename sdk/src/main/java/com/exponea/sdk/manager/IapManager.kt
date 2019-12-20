package com.exponea.sdk.manager

internal interface IapManager {
    fun configure(skuList: List<String>)
    fun startObservingPayments()
    fun stopObservingPayments()
    fun trackPurchase(properties: HashMap<String, Any>)
}
