package com.exponea.example.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.managers.CustomerTokenStorage
import com.exponea.example.utils.isVaildUrl
import com.exponea.example.utils.isValid
import com.exponea.example.utils.onTextChanged
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH
import com.exponea.sdk.models.FlushMode
import kotlinx.android.synthetic.main.activity_authentication.button
import kotlinx.android.synthetic.main.activity_authentication.editTextAdvancedPublicKey
import kotlinx.android.synthetic.main.activity_authentication.editTextApiUrl
import kotlinx.android.synthetic.main.activity_authentication.editTextAuthCode
import kotlinx.android.synthetic.main.activity_authentication.editTextProjectToken
import kotlinx.android.synthetic.main.activity_authentication.editTextRegisteredIds
import kotlinx.android.synthetic.main.activity_authentication.toolbar

class AuthenticationActivity : AppCompatActivity() {

    var projectToken = ""
    var apiUrl = ""
    var authorizationToken = ""
    var advancedPublicKey = ""
    var registeredIds = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        setSupportActionBar(toolbar)

        editTextAuthCode.setText(authorizationToken)
        editTextAdvancedPublicKey.setText(advancedPublicKey)
        editTextRegisteredIds.setText(registeredIds)
        editTextProjectToken.setText(projectToken)
        editTextApiUrl.setText(apiUrl)

        editTextAuthCode.onTextChanged { authorizationToken = it }
        editTextAdvancedPublicKey.onTextChanged { advancedPublicKey = it }
        editTextRegisteredIds.onTextChanged { registeredIds = it }
        editTextProjectToken.onTextChanged { projectToken = it }
        editTextApiUrl.onTextChanged { apiUrl = it }

        button.setOnClickListener {
            if (!editTextProjectToken.isValid() || !editTextAuthCode.isValid() || !editTextApiUrl.isVaildUrl()) {
                Toast.makeText(this, "Empty field", Toast.LENGTH_SHORT).show()
            } else {
                initSdk()
            }
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
        if (editTextRegisteredIds.isValid()) {
            App.instance.registeredIdManager.registeredID = registeredIds
            CustomerTokenStorage.INSTANCE.configure(
                customerIds = hashMapOf(
                    "registered" to (App.instance.registeredIdManager.registeredID ?: "")
                )
            )
        }

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

        // Set up our flushing
        Exponea.flushMode = FlushMode.IMMEDIATE
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
