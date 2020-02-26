package com.exponea.example.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.App
import com.exponea.example.BuildConfig
import com.exponea.example.R
import com.exponea.example.utils.isVaildUrl
import com.exponea.example.utils.isValid
import com.exponea.example.utils.onTextChanged
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import kotlinx.android.synthetic.main.activity_authentication.button
import kotlinx.android.synthetic.main.activity_authentication.editTextApiUrl
import kotlinx.android.synthetic.main.activity_authentication.editTextAuthCode
import kotlinx.android.synthetic.main.activity_authentication.editTextProjectToken
import kotlinx.android.synthetic.main.activity_authentication.editTextRegisteredIds
import kotlinx.android.synthetic.main.activity_authentication.toolbar

class AuthenticationActivity : AppCompatActivity() {

    var authorizationToken = ""
    var projectToken = ""
    var registeredIds = ""
    var apiUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        setSupportActionBar(toolbar)

        editTextAuthCode.onTextChanged { authorizationToken = it }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.auth_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.button_fill_fields -> {
                editTextAuthCode.setText(BuildConfig.AuthorizationToken)
                editTextProjectToken.setText(BuildConfig.DefaultProjectToken)
                editTextApiUrl.setText(BuildConfig.DefaultApiUrl)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initSdk() {
        // Start our exponea configuration
        val configuration = ExponeaConfiguration()
        configuration.authorization = authorizationToken
        configuration.projectToken = projectToken
        configuration.baseURL = apiUrl
        configuration.httpLoggingLevel = ExponeaConfiguration.HttpLoggingLevel.BODY
        configuration.defaultProperties["thisIsADefaultStringProperty"] = "This is a default string value"
        configuration.defaultProperties["thisIsADefaultIntProperty"] = 1

        // Set our customer registration id
        if (editTextRegisteredIds.isValid()) {
            App.instance.registeredIdManager.registeredID = registeredIds
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
