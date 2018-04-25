package com.exponea.sdk.models

data class PropertiesList(
        var properties: HashMap<String, Any>
) {
    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
                Pair("properties", properties)
        )
    }
}