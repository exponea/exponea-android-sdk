package com.exponea.sdk.models

data class PurchasedItem(
        var grossAmount: Double,
        var currency: String,
        var paymentSystem: String,
        var productId: String,
        var productTitle: String,
        var receipt: String?
)