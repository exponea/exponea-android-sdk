package com.exponea.sdk.models

data class PurchasedItem(
    var value: Double,
    var currency: String,
    var paymentSystem: String,
    var productId: String,
    var productTitle: String,
    var receipt: String? = null
) {
    fun toHashMap(): HashMap<String, Any> {
        val hashMap = hashMapOf(
                Pair("brutto", value),
                Pair("currency", currency),
                Pair("payment_system", paymentSystem),
                Pair("item_id", productId),
                Pair("product_title", productTitle)
        )

        receipt?.let { hashMap["receipt"] = it }

        return hashMap
    }
}
