package com.exponea.sdk.models

internal class RecommendationsResponse(
    val data: ArrayList<CustomerRecommendation>?,
    val errors: String,
    val success: Boolean
)
