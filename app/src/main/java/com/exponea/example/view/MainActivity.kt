package com.exponea.example.view

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.fragments.*
import com.exponea.sdk.Exponea
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
