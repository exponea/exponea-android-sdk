package com.exponea.sdk.models

sealed interface IntegrationConfig {
    val baseUrl: String
}

class ProjectConfig(
    override val baseUrl: String = Constants.Repository.baseURL,
    val projectToken: String,
    val authorization: String? = null
) : IntegrationConfig {

    override fun toString(): String =
        "ProjectConfig(baseUrl=$baseUrl, projectToken=$projectToken, authorization=$authorization)"
}

class StreamConfig(
    override val baseUrl: String = Constants.Repository.baseURL,
    val streamId: String
) : IntegrationConfig {

    override fun toString(): String =
        "StreamConfig(baseUrl=$baseUrl, streamId=$streamId)"
}
