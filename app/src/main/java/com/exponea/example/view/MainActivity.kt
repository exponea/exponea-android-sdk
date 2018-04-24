package com.exponea.example.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.view.fragments.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListeners()
        showFragment(0)
    }

    private fun setupListeners() {
        mainBottomNavigation.setOnNavigationItemSelectedListener {
            onNavigationItemClicked(it.itemId)
            return@setOnNavigationItemSelectedListener true
        }
    }

    private fun onNavigationItemClicked(itemID: Int) {
        when (itemID) {
            R.id.actionMain     -> showFragment(0)
            R.id.actionDiscover -> showFragment(1)
            R.id.actionSearch   -> showFragment(2)
            R.id.actionPurchase -> showFragment(3)
            R.id.actionSettings -> showFragment(4)
        }
    }

    private fun showFragment(which: Int) {
        val fragment = when (which) {
            1    -> DiscoverFragment()
            2    -> SearchFragment()
            3    -> PurchaseFragment()
            4    -> SettingsFragment()
            else -> MainFragment()
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment, fragment.javaClass.simpleName)
                .commit()
    }
}
