package com.exponea.example.view

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.exponea.example.R
import com.exponea.example.view.fragments.AnonymizeFragment
import com.exponea.example.view.fragments.FetchFragment
import com.exponea.example.view.fragments.FlushFragment
import com.exponea.example.view.fragments.TrackFragment
import com.exponea.sdk.Exponea
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Examples"

        handleDeeplink(intent)

        if (!Exponea.isInitialized) {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
        } else {
            setupListeners()
        }
        if (savedInstanceState == null) {
            replaceFragment(FetchFragment())
        }

    }

    private fun handleDeeplink(intent: Intent?) {
        intent?.let {
            //get deeplink data
            val data = getIntent().data
            if (data != null) {
                Toast.makeText(this, "Deeplink received: $data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
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
