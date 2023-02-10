package com.exponea.example.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.services.ExampleAppInboxProvider
import com.exponea.example.view.fragments.AnonymizeFragment
import com.exponea.example.view.fragments.FetchFragment
import com.exponea.example.view.fragments.FlushFragment
import com.exponea.example.view.fragments.TrackFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButton
import com.exponea.sdk.models.InAppMessageCallback
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

        // Set log level before first call to SDK function
        Exponea.loggerLevel = Logger.Level.DEBUG
        Exponea.checkPushSetup = true
        Exponea.handleCampaignIntent(intent, applicationContext)

//        Uncomment this section, if you want to test in-app callback
//        Exponea.inAppMessageActionCallback = getInAppMessageCallback()
        Exponea.appInboxProvider = ExampleAppInboxProvider()

        if (Exponea.isInitialized) {
            setupListeners()
        } else {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
        }

        val deeplinkDestination = resolveDeeplinkDestination(intent)

        if (deeplinkDestination != null) {
            handleDeeplinkDestination(deeplinkDestination)
        } else if (savedInstanceState == null) {
            selectTab(BottomTab.Fetch)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val deeplinkDestination = resolveDeeplinkDestination(intent)

        if (deeplinkDestination != null) {
            handleDeeplinkDestination(deeplinkDestination)
        }
    }

    private fun selectTab(tab: BottomTab) {
        navigation.selectedItemId = when (tab) {
            BottomTab.Anonymize -> R.id.actionAnonymize
            BottomTab.Fetch -> R.id.actionMain
            BottomTab.Manual -> R.id.actionSettings
            BottomTab.Track -> R.id.actionPurchase
        }

        val fragment = when (tab) {
            BottomTab.Anonymize -> AnonymizeFragment()
            BottomTab.Fetch -> FetchFragment()
            BottomTab.Manual -> FlushFragment()
            BottomTab.Track -> TrackFragment()
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
    }

    private fun resolveDeeplinkDestination(intent: Intent?): DeeplinkDestination? {
        fun String.toDeeplinkDestination() = when (this) {
            "fetch" -> DeeplinkDestination.Fetch
            "track" -> DeeplinkDestination.Track
            "manual" -> DeeplinkDestination.Manual
            "anonymize" -> DeeplinkDestination.Anonymize
            else -> null
        }

        return if (intent.isViewUrlIntent("http")) {
            intent?.data?.path.orEmpty().toDeeplinkDestination()
        } else if (intent.isViewUrlIntent("exponea")) {
            intent?.data?.host.orEmpty().toDeeplinkDestination()
        } else {
            null
        }
    }

    private fun handleDeeplinkDestination(deeplinkDestination: DeeplinkDestination) {
        when (deeplinkDestination) {
            DeeplinkDestination.Anonymize -> selectTab(BottomTab.Anonymize)
            DeeplinkDestination.Fetch -> selectTab(BottomTab.Fetch)
            DeeplinkDestination.Manual -> selectTab(BottomTab.Manual)
            DeeplinkDestination.Track -> selectTab(BottomTab.Track)
        }
    }

    private fun setupListeners() {
        fun replaceFragment(fragment: androidx.fragment.app.Fragment): Boolean {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit()
            return true
        }

        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.actionMain -> replaceFragment(FetchFragment())
                R.id.actionPurchase -> replaceFragment(TrackFragment())
                R.id.actionAnonymize -> replaceFragment(AnonymizeFragment())
                else -> replaceFragment(FlushFragment())
            }
        }
    }

    private fun getInAppMessageCallback(): InAppMessageCallback {
        return object : InAppMessageCallback {
            override var overrideDefaultBehavior = true
            override var trackActions = false

            override fun inAppMessageAction(
                message: InAppMessage,
                button: InAppMessageButton?,
                interaction: Boolean,
                context: Context
            ) {
                AlertDialog.Builder(context)
                        .setTitle("In app action")
                        .setMessage(" Message id: ${message.id} \n " +
                                "Interaction: $interaction \n ${button?.text} \n ${button?.url}")
                        .setPositiveButton("OK") { _, _ -> }
                        .create()
                        .show()
            }
        }
    }
}
