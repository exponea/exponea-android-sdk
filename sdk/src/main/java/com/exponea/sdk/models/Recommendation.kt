package com.exponea.sdk.models

data class Recommendation(
        var success: Boolean?,
        var results: MutableList<ExportedRecommendation>?
)