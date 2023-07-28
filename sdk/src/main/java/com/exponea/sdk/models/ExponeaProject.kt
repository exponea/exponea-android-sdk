package com.exponea.sdk.models

data class ExponeaProject(
    val baseUrl: String,
    val projectToken: String,
    val authorization: String?,
    val inAppContentBlockPlaceholdersAutoLoad: List<String> = emptyList()
)
