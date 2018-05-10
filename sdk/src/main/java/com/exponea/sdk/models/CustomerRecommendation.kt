package com.exponea.sdk.models

data class CustomerRecommendation(
        var type: String = "recommendation",
        var id: String,
        var size: Int = 10,
        var strategy: String,
        var knowItems: Boolean = false,
        var anti: Boolean = false,
        var items: HashMap<String, Any> = hashMapOf()
) {
        fun toHashMap() : HashMap<String, Any> {
                return hashMapOf(
                        Pair("type", type),
                        Pair("id", id),
                        Pair("size", size),
                        Pair("strategy", strategy),
                        Pair("consider_known_items", knowItems),
                        Pair("anit", anti),
                        Pair("items", items)
                )
        }

}