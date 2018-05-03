package com.exponea.example.view

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.exponea.example.App
import com.exponea.example.BuildConfig
import com.exponea.example.R
import com.exponea.example.utils.isValid
import com.exponea.example.utils.onTextChanged
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.util.Logger
import kotlinx.android.synthetic.main.activity_authentication.*
import java.util.concurrent.TimeUnit

class AuthenticationActivity : AppCompatActivity() {

    var authCode = ""
    var projectToken = ""
    var customerIds = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        editTextAuthCode.onTextChanged { authCode = it  }
        editTextCustomersIds.onTextChanged { customerIds = it }
        editTextProjectToken.onTextChanged { projectToken = it }

        button.setOnClickListener {
            if (!editTextProjectToken.isValid() || !editTextCustomersIds.isValid()
                    || !editTextAuthCode.isValid()) {
                Toast.makeText(this, "Empty field", Toast.LENGTH_SHORT).show()
            } else {
                initSdk()
            }
        }

    }

    private fun initSdk() {
        // Start our exponea configuration
        val configuration = ExponeaConfiguration()
        configuration.authorization = BuildConfig.AuthorizationToken
        configuration.projectToken = BuildConfig.DefaultProjectToken

        // Start our SDK
        Exponea.init(App.instance, configuration)
        // Set our debug level to debug
        Exponea.loggerLevel = Logger.Level.DEBUG
        // Set up our flushing
        Exponea.flushMode = FlushMode.PERIOD
        Exponea.flushPeriod = FlushPeriod(1, TimeUnit.MINUTES)
        startActivity(Intent(this, MainActivity::class.java))
    }

}
