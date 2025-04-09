package com.exponea.example.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.App
import com.exponea.example.databinding.ActivityAuthenticationBinding
import com.exponea.example.managers.CustomerTokenStorage
import com.exponea.example.test.TestWorkerUtil
import com.exponea.example.utils.isVaildUrl
import com.exponea.example.utils.isValid
import com.exponea.example.utils.onTextChanged
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH
import com.exponea.sdk.models.FlushMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {

    var projectToken = ""
    var apiUrl = ""
    var authorizationToken = ""
    var advancedPublicKey = ""
    var registeredIds = ""

    private lateinit var viewBinding: ActivityAuthenticationBinding

    @Inject
    lateinit var testWorkerUtil: TestWorkerUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbar)

        viewBinding.editTextAuthCode.setText(authorizationToken)
        viewBinding.editTextAdvancedPublicKey.setText(advancedPublicKey)
        viewBinding.editTextRegisteredIds.setText(registeredIds)
        viewBinding.editTextProjectToken.setText(projectToken)
        viewBinding.editTextApiUrl.setText(apiUrl)

        viewBinding.editTextAuthCode.onTextChanged { authorizationToken = it }
        viewBinding.editTextAdvancedPublicKey.onTextChanged { advancedPublicKey = it }
        viewBinding.editTextRegisteredIds.onTextChanged { registeredIds = it }
        viewBinding.editTextProjectToken.onTextChanged { projectToken = it }
        viewBinding.editTextApiUrl.onTextChanged { apiUrl = it }

        viewBinding.authenticateButton.setOnClickListener {
            if (!viewBinding.editTextProjectToken.isValid() ||
                !viewBinding.editTextAuthCode.isValid() ||
                !viewBinding.editTextApiUrl.isVaildUrl()
            ) {
                Toast.makeText(this, "Empty field", Toast.LENGTH_SHORT).show()
            } else {
                testWorkerUtil.startWork()
                initSdk()
            }
        }

        viewBinding.clearLocalDataButton.setOnClickListener {
            Exponea.clearLocalCustomerData()
        }
    }

    private fun initSdk() {
        // Start our exponea configuration
        val configuration = ExponeaConfiguration()
        configuration.authorization = authorizationToken
        configuration.advancedAuthEnabled = advancedPublicKey.isNotBlank()
        configuration.projectToken = projectToken
        configuration.baseURL = apiUrl
        configuration.httpLoggingLevel = ExponeaConfiguration.HttpLoggingLevel.BODY
        configuration.defaultProperties["thisIsADefaultStringProperty"] = "This is a default string value"
        configuration.defaultProperties["thisIsADefaultIntProperty"] = 1
        configuration.automaticPushNotification = true
        configuration.tokenTrackFrequency = EVERY_LAUNCH
        configuration.pushChannelId = "123"

        // Prepare Example Advanced Auth
        CustomerTokenStorage.INSTANCE.configure(
            host = apiUrl,
            projectToken = projectToken,
            publicKey = advancedPublicKey,
            customerIds = null,
            expiration = null
        )

        // Set our customer registration id
        if (viewBinding.editTextRegisteredIds.isValid()) {
            App.instance.registeredIdManager.registeredID = registeredIds
            CustomerTokenStorage.INSTANCE.configure(
                customerIds = hashMapOf(
                    "registered" to (App.instance.registeredIdManager.registeredID ?: "")
                )
            )
        }

        // Set up our flushing
        Exponea.flushMode = FlushMode.IMMEDIATE
        Exponea.checkPushSetup = true

        // Start our SDK
        // Exponea.initFromFile(App.instance)
        try {
            Exponea.init(App.instance, configuration)
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Error configuring SDK")
                .setMessage(e.localizedMessage)
                .setPositiveButton("OK") { _, _ -> }
                .create()
                .show()
            return
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
