package com.exponea.sdk.view

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.services.OnIntegrationStoppedCallback

class AppInboxListActivity : AppCompatActivity(), OnIntegrationStoppedCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.message_inbox_list_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.exponea_inbox_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val listFragment = Exponea.getAppInboxListFragment(this)
        if (listFragment == null) {
            finish()
            return
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, listFragment)
            .commit()
        Exponea.deintegration.registerForIntegrationStopped(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onIntegrationStopped() {
        finish()
    }

    override fun onDestroy() {
        Exponea.deintegration.unregisterForIntegrationStopped(this)
        super.onDestroy()
    }
}
