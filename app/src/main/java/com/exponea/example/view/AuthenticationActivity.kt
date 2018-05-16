package com.exponea.example.view

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

    var authorizationToken = ""
    var projectToken = ""
    var customerIds = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        setSupportActionBar(toolbar)
        editTextAuthCode.onTextChanged { authorizationToken = it  }
        editTextCustomersIds.onTextChanged { customerIds = it }
        editTextProjectToken.onTextChanged { projectToken = it }

        button.setOnClickListener {
            if (!editTextProjectToken.isValid() || !editTextAuthCode.isValid()) {
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
       return when(item?.itemId) {
            R.id.button_fill_fields -> {
               editTextAuthCode.setText(BuildConfig.AuthorizationToken)
               editTextProjectToken.setText(BuildConfig.DefaultProjectToken)
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

        // Set our customer id
        if (editTextCustomersIds.isValid()) {
            App.instance.userIdManager.uniqueUserID = customerIds
        }

        // Start our SDK
        Exponea.init(App.instance, configuration)
        // Set our debug level to debug
        Exponea.loggerLevel = Logger.Level.DEBUG
        // Set up our flushing
        Exponea.flushMode = FlushMode.PERIOD
        Exponea.flushPeriod = FlushPeriod(1, TimeUnit.MINUTES)

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

}
