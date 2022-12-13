package com.exponea.sdk.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.models.MessageItem
import kotlinx.android.synthetic.main.message_inbox_list_activity.toolbar

internal class AppInboxDetailActivity : AppCompatActivity() {
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
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val messageId = intent.getStringExtra(MESSAGE_ID)
        if (messageId == null) {
            finish()
            return
        }
        Exponea.fetchAppInboxItem(messageId, { message ->
            supportActionBar?.title = message?.content?.title
                ?: getString(R.string.exponea_inbox_defaultTitle)
        })
        val detailFragment = Exponea.getAppInboxDetailFragment(this, messageId)
        if (detailFragment == null) {
            finish()
            return
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, detailFragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
