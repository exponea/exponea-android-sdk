package com.exponea.example.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.view.fragments.AnonymizeFragment
import com.exponea.example.view.fragments.FetchFragment
import com.exponea.example.view.fragments.FlushFragment
import com.exponea.example.view.fragments.TrackFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isViewUrlIntent
import kotlinx.android.synthetic.main.activity_main.navigation
import kotlinx.android.synthetic.main.activity_main.toolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Examples"

        if (intent.isViewUrlIntent("http") || intent.isViewUrlIntent("exponea")) {
            Toast.makeText(this, "Deep link received from ${intent?.data?.host}, " +
                    "path is ${intent?.data?.path}", Toast.LENGTH_LONG).show()
        }

        // Set log level before first call to SDK function
        Exponea.loggerLevel = Logger.Level.DEBUG

        Exponea.handleCampaignIntent(intent, applicationContext)

        if (Exponea.isInitialized) {
            setupListeners()
        } else {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
        }
        if (savedInstanceState == null) {
            replaceFragment(FetchFragment())
        }
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment): Boolean {
        supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.container, fragment)
                ?.commit()
        return true
    }

    private fun setupListeners() {
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.actionMain -> replaceFragment(FetchFragment())
                R.id.actionPurchase -> replaceFragment(TrackFragment())
                R.id.actionAnonymize -> replaceFragment(AnonymizeFragment())
                else -> replaceFragment(FlushFragment())
            }
        }
    }
}
