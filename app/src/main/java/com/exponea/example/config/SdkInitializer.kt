package com.exponea.example.config

import com.exponea.example.App
import com.exponea.example.managers.CustomerTokenStorage
import com.exponea.example.managers.LocalJwtTokenGenerator
import com.exponea.example.model.Constants
import com.exponea.example.model.SdkSetupState
import com.exponea.example.status.SdkLifecycleState
import com.exponea.example.status.SdkStatusStore
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIdentity
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.SdkAuthCallback
import com.exponea.sdk.models.SdkAuthError
import com.exponea.sdk.models.StreamConfig

enum class IntegrationMode { PROJECT, STREAM }

data class AuthFormState(
    val mode: IntegrationMode = IntegrationMode.PROJECT,
    val projectToken: String = "",
    val authorization: String = "",
    val advancedAuthKey: String = "",
    val streamId: String = "",
    val jwtKeyId: String = "",
    val jwtSecret: String = "",
    val registeredId: String = "",
    val apiUrl: String = "",
    val applicationId: String = ""
)

sealed class SdkInitResult {
    data object Success : SdkInitResult()
    data class Error(val message: String?) : SdkInitResult()
}

object SdkInitializer {

    fun init(form: AuthFormState): SdkInitResult {
        val configuration = ExponeaConfiguration()
        configuration.integrationConfig = when (form.mode) {
            IntegrationMode.PROJECT -> ProjectConfig(
                baseUrl = form.apiUrl,
                projectToken = form.projectToken,
                authorization = form.authorization
            )
            IntegrationMode.STREAM -> StreamConfig(
                baseUrl = form.apiUrl,
                streamId = form.streamId
            )
        }
        configuration.advancedAuthEnabled = form.advancedAuthKey.isNotBlank()
        configuration.httpLoggingLevel = ExponeaConfiguration.HttpLoggingLevel.BODY
        configuration.defaultProperties["thisIsADefaultStringProperty"] = "This is a default string value"
        configuration.defaultProperties["thisIsADefaultIntProperty"] = 1
        configuration.automaticPushNotification = true
        configuration.tokenTrackFrequency = ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH
        configuration.pushChannelId = "123"
        if (form.applicationId.isNotEmpty()) {
            configuration.applicationId = form.applicationId
        }

        val customerIds: HashMap<String, String> = if (form.registeredId.isNotBlank()) {
            App.instance.registeredIdManager.registeredID = form.registeredId
            hashMapOf(Constants.CUSTOMER_ID_REGISTERED to form.registeredId)
        } else {
            hashMapOf()
        }

        if (form.mode == IntegrationMode.PROJECT && form.advancedAuthKey.isNotBlank()) {
            CustomerTokenStorage.INSTANCE.configure(
                host = form.apiUrl,
                projectToken = form.projectToken,
                publicKey = form.advancedAuthKey,
                customerIds = customerIds.takeIf { it.isNotEmpty() },
                expiration = null
            )
        }

        if (form.mode == IntegrationMode.STREAM &&
            form.jwtKeyId.isNotBlank() && form.jwtSecret.isNotBlank()
        ) {
            LocalJwtTokenGenerator.INSTANCE.configure(
                secret = form.jwtSecret,
                kid = form.jwtKeyId
            )
        }

        Exponea.flushMode = FlushMode.IMMEDIATE
        Exponea.checkPushSetup = true

        val customerIdentity = if (customerIds.isNotEmpty()) {
            CustomerIdentity(
                customerIds = customerIds,
                sdkAuthToken = if (LocalJwtTokenGenerator.INSTANCE.isConfigured()) {
                    LocalJwtTokenGenerator.INSTANCE.generateToken(customerIds)
                } else {
                    null
                }
            )
        } else {
            null
        }

        return try {
            Exponea.init(App.instance, configuration, customerIdentity)
            SdkSetupState.storeInitConfig(form, configuration)
            if (customerIdentity != null) {
                SdkSetupState.isCustomerIdentified = true
            }
            SdkStatusStore.set(
                if (customerIdentity != null) {
                    SdkLifecycleState.IDENTIFIED
                } else {
                    SdkLifecycleState.ANONYMOUS
                }
            )
            if (LocalJwtTokenGenerator.INSTANCE.isConfigured()) {
                Exponea.sdkAuthCallback = object : SdkAuthCallback {
                    override fun onAuthFailure(error: SdkAuthError) {
                        if (SdkSetupState.isCustomerIdentified) {
                            val ids = mapOf(
                                Constants.CUSTOMER_ID_REGISTERED to
                                    App.instance.registeredIdManager.registeredID
                            )
                            LocalJwtTokenGenerator.INSTANCE.generateToken(ids)?.let { token ->
                                Exponea.setSdkAuthToken(token)
                            }
                        }
                    }
                }
            }
            SdkInitResult.Success
        } catch (e: Exception) {
            SdkStatusStore.set(SdkLifecycleState.NOT_INITIALIZED)
            SdkInitResult.Error(e.localizedMessage)
        }
    }
}
