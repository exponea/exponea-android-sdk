package com.exponea.example.view

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.sdk.Exponea
import kotlinx.android.synthetic.main.activity_authentication.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Examples"

        if (!Exponea.isInitialized) {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
        } else {
            setupListeners()
        }

    }

    private fun setupListeners() {
        buttonTrackCustomerEvent.setOnClickListener {
            EventTrackingActivity.startAsIntent(this, Constants.Events.EVENT_TRACK_CUSTOMER)
        }

        buttonTrackEvent.setOnClickListener{
            EventTrackingActivity.startAsIntent(this, Constants.Events.EVENT_UPDATE_CUSTOMER)
        }


    }

}
