package com.exponea.sdk.models

data class CustomerRecommendation(
        var type: String,
        var id: String,
        var size: Int?,
        var strategy: String?,
        var knowItems: Boolean?,
        var anti: Boolean?,
        var items: HashMap<String, String>?
)