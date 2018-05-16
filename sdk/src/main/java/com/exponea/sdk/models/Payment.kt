package com.exponea.sdk.models

data class Payment(
        val currency: String,
        val amount: Double,
        val itemName: String,
        val itemType: String
) {
    fun toHashMap() : HashMap<String, Any> {
        return hashMapOf(
                Pair("currency", currency),
                Pair("amount", amount),
                Pair("item_name", itemName),
                Pair("item_type", itemType)
        )
    }
}