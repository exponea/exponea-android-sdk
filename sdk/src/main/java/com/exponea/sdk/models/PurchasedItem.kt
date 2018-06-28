package com.exponea.sdk.models

data class PurchasedItem(
        var value: Double,
        var currency: String,
        var paymentSystem: String,
        var productId: String,
        var productTitle: String,
        var receipt: String? = null,
        var deviceModel: String? = null,
        var deviceType: String? = null,
        var ip: String? = null,
        var osName: String? = null,
        var osVersion: String? = null,
        var sdk: String? = null,
        var sdkVersion: String? = null
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
        deviceModel?.let { hashMap["device_model"] = it }
        deviceType?.let { hashMap["device_type"] = it }
        ip?.let { hashMap["ip"] = it }
        osName?.let { hashMap["os_name"] = it }
        osVersion?.let { hashMap["os_version"] = it }
        sdk?.let { hashMap["sdk"] = it }
        sdkVersion?.let { hashMap["sdk_version"] = it }

        return hashMap
    }
}