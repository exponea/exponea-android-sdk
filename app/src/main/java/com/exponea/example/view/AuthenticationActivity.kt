package com.exponea.example.view

import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.exponea.example.App
import com.exponea.example.callbacks.ExampleLoggerCallback
import com.exponea.example.databinding.ActivityAuthenticationBinding
import com.exponea.example.managers.CustomerTokenStorage
import com.exponea.example.managers.LocalJwtTokenGenerator
import com.exponea.example.models.Constants.CUSTOMER_ID_REGISTERED
import com.exponea.example.models.SdkSetupState
import com.exponea.example.utils.isVaildUrl
import com.exponea.example.utils.isValid
import com.exponea.example.utils.onTextChanged
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIdentity
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.SdkAuthCallback
import com.exponea.sdk.models.SdkAuthError
import com.exponea.sdk.models.StreamConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AuthenticationActivity : AppCompatActivity() {

    private var projectToken = ""
    private var apiUrl = ""
    private var authorizationToken = ""
    private var streamId = ""
    private var advancedPublicKey = ""
    private var jwtKeyId = ""
    private var jwtSecret = ""
    private var registeredIds = ""
    private var applicationId = ""

    private lateinit var viewBinding: ActivityAuthenticationBinding
    private val integrationConfigOptions = IntegrationConfigOption.entries
    private var integrationConfig: IntegrationConfigOption = integrationConfigOptions[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbar)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            integrationConfigOptions.map { it.displayName }
        )
        viewBinding.integrationConfigDropdown.setAdapter(adapter)

        viewBinding.integrationConfigDropdown.setText(integrationConfig.displayName, false)
        viewBinding.editTextAuthCode.setText(authorizationToken)
        viewBinding.editTextAdvancedPublicKey.setText(advancedPublicKey)
        viewBinding.editTextRegisteredIds.setText(registeredIds)
        viewBinding.editTextProjectToken.setText(projectToken)
        viewBinding.editTextStreamIdLayout.visibility = GONE
        viewBinding.editTextStreamId.setText(streamId)
        viewBinding.editTextJwtKeyIdLayout.visibility = GONE
        viewBinding.editTextJwtKeyId.setText(jwtKeyId)
        viewBinding.editTextJwtSecretLayout.visibility = GONE
        viewBinding.editTextJwtSecret.setText(jwtSecret)
        viewBinding.editTextApiUrl.setText(apiUrl)
        viewBinding.editTextApplicationId.setText(applicationId)

        viewBinding.integrationConfigDropdown.setOnItemClickListener { _, _, position, _ ->
            integrationConfig = integrationConfigOptions[position]
            val projectSpecificLayouts = listOf(
                viewBinding.editTextProjectTokenLayout,
                viewBinding.editTextAuthCodeLayout,
                viewBinding.editTextAdvancedPublicKeyLayout
            )
            val streamSpecificLayouts = listOf(
                viewBinding.editTextStreamIdLayout,
                viewBinding.editTextJwtKeyIdLayout,
                viewBinding.editTextJwtSecretLayout
            )
            when (integrationConfig) {
                IntegrationConfigOption.PROJECT_CONFIG -> {
                    projectSpecificLayouts.forEach { it.visibility = VISIBLE }
                    streamSpecificLayouts.forEach { it.visibility = GONE }
                }

                IntegrationConfigOption.STREAM_CONFIG -> {
                    streamSpecificLayouts.forEach { it.visibility = VISIBLE }
                    projectSpecificLayouts.forEach { it.visibility = GONE }
                }
            }
        }
        viewBinding.editTextStreamId.onTextChanged(validate = true) { streamId = it }
        viewBinding.editTextProjectToken.onTextChanged(validate = true) { projectToken = it }
        viewBinding.editTextAuthCode.onTextChanged(validate = true) { authorizationToken = it }
        viewBinding.editTextAdvancedPublicKey.onTextChanged { advancedPublicKey = it }
        viewBinding.editTextJwtKeyId.onTextChanged { jwtKeyId = it }
        viewBinding.editTextJwtSecret.onTextChanged { jwtSecret = it }
        viewBinding.editTextApiUrl.onTextChanged(validate = true) { apiUrl = it }
        viewBinding.editTextRegisteredIds.onTextChanged { registeredIds = it }
        viewBinding.editTextApplicationId.onTextChanged { applicationId = it }

        viewBinding.authenticateButton.setOnClickListener {
            if ((viewBinding.editTextProjectTokenLayout.isVisible &&
                        !viewBinding.editTextProjectToken.isValid() &&
                        !viewBinding.editTextAuthCode.isValid()
                        ) ||
                (viewBinding.editTextStreamIdLayout.isVisible && !viewBinding.editTextStreamId.isValid()) ||
                !viewBinding.editTextApiUrl.isVaildUrl()
            ) {
                Toast.makeText(this, "Empty field!", Toast.LENGTH_SHORT).show()
            } else if (jwtKeyId.isNotBlank() != jwtSecret.isNotBlank()) {
                Toast.makeText(
                    this,
                    "JWT Key ID and JWT Secret must both be provided or both empty.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                initSdk()
            }
        }

        viewBinding.clearLocalDataButton.setOnClickListener {
            Exponea.clearLocalCustomerData()
            Exponea.unregisterLoggerCallback(ExampleLoggerCallback)
            SdkSetupState.reset()
        }
    }

    private fun initSdk() {
        // Start our exponea configuration
        val configuration = ExponeaConfiguration()
        configuration.integrationConfig = when (integrationConfig) {
            IntegrationConfigOption.PROJECT_CONFIG ->
                ProjectConfig(baseUrl = apiUrl, projectToken = projectToken, authorization = authorizationToken)

            IntegrationConfigOption.STREAM_CONFIG ->
                StreamConfig(baseUrl = apiUrl, streamId = streamId)
        }
        configuration.advancedAuthEnabled = advancedPublicKey.isNotBlank()
        configuration.httpLoggingLevel = ExponeaConfiguration.HttpLoggingLevel.BODY
        configuration.defaultProperties["thisIsADefaultStringProperty"] = "This is a default string value"
        configuration.defaultProperties["thisIsADefaultIntProperty"] = 1
        configuration.automaticPushNotification = true
        configuration.tokenTrackFrequency = EVERY_LAUNCH
        configuration.pushChannelId = "123"

        if (applicationId.isNotEmpty()) {
            configuration.applicationId = applicationId
        }

        // Set our customer registration id if provided
        val customerIds: HashMap<String, String> = if (registeredIds.isNotBlank()) {
            App.instance.registeredIdManager.registeredID = registeredIds
            hashMapOf(CUSTOMER_ID_REGISTERED to registeredIds)
        } else {
            hashMapOf()
        }

        // Configure our CustomerTokenStorage for ProjectConfig if public key was provided
        if (integrationConfig == IntegrationConfigOption.PROJECT_CONFIG && advancedPublicKey.isNotBlank()) {
            // Prepare Example Advanced Auth
            CustomerTokenStorage.INSTANCE.configure(
                host = apiUrl,
                projectToken = projectToken,
                publicKey = advancedPublicKey,
                customerIds = customerIds.takeIf { it.isNotEmpty() },
                expiration = null
            )
        }

        // Configure local JWT generator for StreamConfig if JWT credentials were provided
        if (integrationConfig == IntegrationConfigOption.STREAM_CONFIG &&
            jwtKeyId.isNotBlank() && jwtSecret.isNotBlank()
        ) {
            LocalJwtTokenGenerator.INSTANCE.configure(secret = jwtSecret, kid = jwtKeyId)
        }

        // Set up our flushing
        Exponea.flushMode = FlushMode.IMMEDIATE
        Exponea.checkPushSetup = true

        // Optional customer identity
        val customerIdentity = if (customerIds.isNotEmpty()) {
            CustomerIdentity(
                customerIds = customerIds,
                sdkAuthToken = if (LocalJwtTokenGenerator.INSTANCE.isConfigured())
                    LocalJwtTokenGenerator.INSTANCE.generateToken(customerIds)
                else null
            )
        } else null

        SdkSetupState.isStreamConfig = integrationConfig == IntegrationConfigOption.STREAM_CONFIG

        // Start our SDK
        try {
            Exponea.init(App.instance, configuration, customerIdentity)
                .also {
                    if (customerIdentity != null) {
                        SdkSetupState.isCustomerIdentified = true
                    }
                }
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Error configuring SDK")
                .setMessage(e.localizedMessage)
                .setPositiveButton("OK") { _, _ -> }
                .create()
                .show()
            return
        }

        // Observe SDK warnings/errors via a LoggerCallback
        Exponea.registerLoggerCallback(ExampleLoggerCallback)

        // Wire up local JWT generation for StreamConfig when a kid/secret were provided
        if (LocalJwtTokenGenerator.INSTANCE.isConfigured()) {
            Exponea.sdkAuthCallback = object : SdkAuthCallback {
                override fun onAuthFailure(error: SdkAuthError) {
                    if (SdkSetupState.isCustomerIdentified) {
                        val customerIds = mapOf(CUSTOMER_ID_REGISTERED to App.instance.registeredIdManager.registeredID)
                        LocalJwtTokenGenerator.INSTANCE.generateToken(customerIds)?.let { token ->
                            Exponea.setSdkAuthToken(token)
                        }
                    }
                }
            }
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private enum class IntegrationConfigOption(val displayName: String) {
        PROJECT_CONFIG("Project Config"),
        STREAM_CONFIG("Stream Config")
    }
}
