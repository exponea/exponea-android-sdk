package com.exponea.example.config

import android.content.Intent

object EnvConfiguration {

    const val EXTRA_BASE_URL = "EXPONEA_BASE_URL"
    const val EXTRA_PROJECT_TOKEN = "EXPONEA_PROJECT_TOKEN"
    const val EXTRA_AUTH_TOKEN = "EXPONEA_AUTH_TOKEN"
    const val EXTRA_STREAM_ID = "EXPONEA_STREAM_ID"
    const val EXTRA_CUSTOMER_ID = "EXPONEA_CUSTOMER_ID"
    const val EXTRA_INTEGRATION_MODE = "EXPONEA_INTEGRATION_MODE"
    const val EXTRA_JWT_KEY_ID = "EXPONEA_JWT_KEY_ID"
    const val EXTRA_JWT_SECRET = "EXPONEA_JWT_SECRET"
    const val EXTRA_APPLICATION_ID = "EXPONEA_APPLICATION_ID"

    fun resolve(intent: Intent?): AuthFormState? {
        val fromIntent = intent?.let { readFromIntent(it) }
        return fromIntent?.takeIf { isComplete(it) }
    }

    fun isComplete(form: AuthFormState): Boolean {
        if (form.apiUrl.isBlank()) return false
        return when (form.mode) {
            IntegrationMode.PROJECT ->
                form.projectToken.isNotBlank() && form.authorization.isNotBlank()
            IntegrationMode.STREAM -> form.streamId.isNotBlank()
        }
    }

    private fun readFromIntent(intent: Intent): AuthFormState? {
        val baseUrl = intent.getStringExtra(EXTRA_BASE_URL).orEmpty()
        if (baseUrl.isBlank()) return null
        return buildForm(
            mode = intent.getStringExtra(EXTRA_INTEGRATION_MODE),
            baseUrl = baseUrl,
            projectToken = intent.getStringExtra(EXTRA_PROJECT_TOKEN),
            authToken = intent.getStringExtra(EXTRA_AUTH_TOKEN),
            streamId = intent.getStringExtra(EXTRA_STREAM_ID),
            customerId = intent.getStringExtra(EXTRA_CUSTOMER_ID),
            jwtKeyId = intent.getStringExtra(EXTRA_JWT_KEY_ID),
            jwtSecret = intent.getStringExtra(EXTRA_JWT_SECRET),
            applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)
        )
    }

    private fun buildForm(
        mode: String?,
        baseUrl: String?,
        projectToken: String?,
        authToken: String?,
        streamId: String?,
        customerId: String?,
        jwtKeyId: String?,
        jwtSecret: String?,
        applicationId: String?
    ): AuthFormState {
        val integrationMode = when (mode?.lowercase()) {
            "stream" -> IntegrationMode.STREAM
            else -> IntegrationMode.PROJECT
        }
        return AuthFormState(
            mode = integrationMode,
            projectToken = projectToken.orEmpty().trim(),
            authorization = authToken.orEmpty().trim(),
            streamId = streamId.orEmpty().trim(),
            jwtKeyId = jwtKeyId.orEmpty().trim(),
            jwtSecret = jwtSecret.orEmpty().trim(),
            registeredId = customerId.orEmpty().trim(),
            apiUrl = baseUrl.orEmpty().trim(),
            applicationId = applicationId.orEmpty().trim()
        )
    }
}
