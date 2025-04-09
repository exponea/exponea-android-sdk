package com.exponea.sdk.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.util.ConversionUtils
import com.exponea.sdk.util.Logger

internal class AppInboxDetailActivity : AppCompatActivity(), OnIntegrationStoppedCallback {
    companion object {
        public val MESSAGE_ID = "MessageID"
        fun buildIntent(context: Context, item: MessageItem): Intent {
            return Intent(context, AppInboxDetailActivity::class.java).apply {
                putExtra(MESSAGE_ID, item.id)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.message_inbox_detail_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val messageId = intent.getStringExtra(MESSAGE_ID)
        if (messageId == null) {
            finish()
            return
        }
        Exponea.fetchAppInboxItem(messageId) { message ->
            supportActionBar?.title = message?.content?.title
                    ?: getString(R.string.exponea_inbox_defaultTitle)
        }
        Exponea.getComponent()?.exponeaConfiguration?.appInboxDetailImageInset?.let {
            try {
                val detailContainer = findViewById<FrameLayout>(R.id.container)
                val layoutParams = detailContainer.layoutParams as RelativeLayout.LayoutParams
                layoutParams.topMargin = ConversionUtils.dpToPx(it)
                detailContainer.layoutParams = layoutParams
            } catch (e: Exception) {
                Logger.e(this, """
                    App Inbox detail screen changed in layout, unable to apply `appInboxDetailImageInset`
                """.trimIndent())
            }
        }
        val detailFragment = Exponea.getAppInboxDetailFragment(this, messageId)
        if (detailFragment == null) {
            finish()
            return
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, detailFragment)
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
