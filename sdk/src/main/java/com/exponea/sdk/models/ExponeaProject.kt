package com.exponea.sdk.models

@Deprecated("Please use ProjectConfig instead", ReplaceWith("ProjectConfig"))
data class ExponeaProject(
    val baseUrl: String,
    val projectToken: String,
    val authorization: String?,
    val inAppContentBlockPlaceholdersAutoLoad: List<String> = emptyList()
)
