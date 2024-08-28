package com.exponea.example.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.R.id
import com.exponea.example.services.ExampleAppInboxProvider
import com.exponea.example.view.BottomTab.Anonymize
import com.exponea.example.view.BottomTab.Fetch
import com.exponea.example.view.BottomTab.InAppContentBlock
import com.exponea.example.view.BottomTab.Manual
import com.exponea.example.view.BottomTab.Track
import com.exponea.example.view.fragments.AnonymizeFragment
import com.exponea.example.view.fragments.FetchFragment
import com.exponea.example.view.fragments.FlushFragment
import com.exponea.example.view.fragments.InAppContentBlocksFragment
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
            Anonymize -> id.actionAnonymize
            Fetch -> id.actionMain
            Manual -> id.actionSettings
            Track -> id.actionPurchase
            InAppContentBlock -> id.actionInAppContentBlock
        }

        val fragment = when (tab) {
            Anonymize -> AnonymizeFragment()
            Fetch -> FetchFragment()
            Manual -> FlushFragment()
            Track -> TrackFragment()
            InAppContentBlock -> InAppContentBlocksFragment()
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
    }

    private fun resolveDeeplinkDestination(intent: Intent?): DeeplinkDestination? {
        fun String.toDeeplinkDestination() = when {
            this.contains("track") -> DeeplinkDestination.Track
            this.contains("flush") -> DeeplinkDestination.Manual
            this.contains("fetch") -> DeeplinkDestination.Fetch
            this.contains("inappcb") -> DeeplinkDestination.InAppCb
            this.contains("anonymize") -> DeeplinkDestination.Anonymize
            else -> null
        }
        return if (intent.isViewUrlIntent("http")) {
            intent?.data?.path.orEmpty().toDeeplinkDestination()
        } else if (intent.isViewUrlIntent("exponea")) {
            intent?.data?.path.orEmpty().toDeeplinkDestination()
        } else {
            null
        }
    }

    private fun handleDeeplinkDestination(deeplinkDestination: DeeplinkDestination) {
        when (deeplinkDestination) {
            DeeplinkDestination.Anonymize -> selectTab(Anonymize)
            DeeplinkDestination.Fetch -> selectTab(Fetch)
            DeeplinkDestination.Manual -> selectTab(Manual)
            DeeplinkDestination.Track -> selectTab(Track)
            DeeplinkDestination.InAppCb -> selectTab(InAppContentBlock)
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
                R.id.actionInAppContentBlock -> replaceFragment(InAppContentBlocksFragment())
                else -> replaceFragment(FlushFragment())
            }
        }
    }

    private fun getInAppMessageCallback(): InAppMessageCallback {
        return object : InAppMessageCallback {
            override var overrideDefaultBehavior = true
            override var trackActions = false

            override fun inAppMessageShown(message: InAppMessage, context: Context) {
                Logger.i(this, "In app message ${message.name} has been shown")
            }

            override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {
                Logger.e(this, "Error occurred '$errorMessage' while showing in app message ${message?.name}")
            }

            override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {
                AlertDialog.Builder(context)
                    .setTitle("In app action clicked")
                    .setMessage(" Message id: ${message.id} \n " +
                        "Interaction: \n ${button.text} \n ${button.url}")
                    .setPositiveButton("OK") { _, _ -> }
                    .create()
                    .show()
            }

            override fun inAppMessageCloseAction(
                message: InAppMessage,
                button: InAppMessageButton?,
                interaction: Boolean,
                context: Context
            ) {
                AlertDialog.Builder(context)
                    .setTitle("In app closed")
                    .setMessage(" Message id: ${message.id} \n " +
                        "Interaction: $interaction \n ${button?.text} \n ${button?.url}")
                    .setPositiveButton("OK") { _, _ -> }
                    .create()
                    .show()
            }
        }
    }
}
