package com.exponea.example.view

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.view.fragments.MainFragment
import com.exponea.example.view.fragments.PurchaseFragment
import com.exponea.example.view.fragments.SettingsFragment
import com.exponea.sdk.Exponea
import kotlinx.android.synthetic.main.activity_main.*

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
        if (savedInstanceState == null) {
            replaceFragment(MainFragment())
        }

    }

    private fun replaceFragment(fragment: Fragment) : Boolean {
        supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.container, fragment)
                ?.commit()
        return true
    }

    private fun setupListeners() {
        navigation.setOnNavigationItemSelectedListener{
            when (it.itemId) {
                R.id.actionMain -> replaceFragment(MainFragment())
                R.id.actionPurchase -> replaceFragment(PurchaseFragment())
                else -> replaceFragment(SettingsFragment())
            }
        }
    }

}
