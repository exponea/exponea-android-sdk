package com.exponea.sdk.models

data class PropertiesList(
        var properties: HashMap<String, Any>
) {

    operator fun set(key: String, value: Any?) {
        value?.let {
            properties[key] = it
        }
    }

    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
                Pair("properties", properties)
        )
    }
}
