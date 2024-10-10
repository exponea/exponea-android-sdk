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
import com.exponea.sdk.models.ExponeaNotificationActionType
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButton
import com.exponea.sdk.models.InAppMessageCallback
import com.exponea.sdk.models.PushNotificationDelegate
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isResumedActivity
import com.exponea.sdk.util.isViewUrlIntent
import kotlinx.android.synthetic.main.activity_main.navigation
import kotlinx.android.synthetic.main.activity_main.toolbar

class MainActivity : AppCompatActivity() {

    private val contentSegmentsCallback = object : SegmentationDataCallback() {
        override val exposingCategory = "content"
        override val includeFirstLoad = false
        override fun onNewData(segments: List<Segment>) {
            Logger.i(
                this@MainActivity,
                "Segments: New for category $exposingCategory with IDs: $segments"
            )
        }
    }

    private val discoverySegmentsCallback = object : SegmentationDataCallback() {
        override val exposingCategory = "discovery"
        override val includeFirstLoad = false
        override fun onNewData(segments: List<Segment>) {
            Logger.i(
                this@MainActivity,
                "Segments: New for category $exposingCategory with IDs: $segments"
            )
        }
    }

    private val merchandisingSegmentsCallback = object : SegmentationDataCallback() {
        override val exposingCategory = "merchandising"
        override val includeFirstLoad = false
        override fun onNewData(segments: List<Segment>) {
            Logger.i(
                this@MainActivity,
                "Segments: New for category $exposingCategory with IDs: $segments"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Examples"

        // Set log level before first call to SDK function
        Exponea.loggerLevel = Logger.Level.VERBOSE
        Exponea.checkPushSetup = true
        Exponea.handleCampaignIntent(intent, applicationContext)
        Exponea.pushNotificationsDelegate = object : PushNotificationDelegate {
            override fun onSilentPushNotificationReceived(notificationData: Map<String, Any>) {
                informPushNotificationAction("Silent", "received", notificationData)
            }

            override fun onPushNotificationReceived(notificationData: Map<String, Any>) {
                informPushNotificationAction("Normal", "received", notificationData)
            }

            override fun onPushNotificationOpened(
                action: ExponeaNotificationActionType,
                url: String?,
                notificationData: Map<String, Any>
            ) {
                informPushNotificationAction(action.name, "clicked $url", notificationData)
            }

            private fun informPushNotificationAction(
                notifType: String,
                notifFlow: String,
                notificationData: Map<String, Any>
            ) {
                val message = """
                    $notifType Push data $notifFlow:
                    ${notificationData.entries.joinToString { "${it.key}: ${it.value}" }}
                    """.trimIndent()
                if (this@MainActivity.isResumedActivity()) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("$notifType Push notification $notifFlow")
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ -> }
                        .show()
                } else {
                    Logger.i(this, message)
                }
            }
        }

//        Uncomment this section, if you want to test in-app callback
//        Exponea.inAppMessageActionCallback = getInAppMessageCallback()
        Exponea.appInboxProvider = ExampleAppInboxProvider()

        if (Exponea.isInitialized) {
            setupListeners()
        } else {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
            return
        }

        val deeplinkDestination = resolveDeeplinkDestination(intent)

        if (deeplinkDestination != null) {
            handleDeeplinkDestination(deeplinkDestination)
        } else if (savedInstanceState == null) {
            selectTab(BottomTab.Fetch)
        }

        Exponea.registerSegmentationDataCallback(discoverySegmentsCallback)
        Exponea.registerSegmentationDataCallback(contentSegmentsCallback)
        Exponea.registerSegmentationDataCallback(merchandisingSegmentsCallback)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val deeplinkDestination = resolveDeeplinkDestination(intent)

        if (deeplinkDestination != null) {
            handleDeeplinkDestination(deeplinkDestination)
        }
    }

    override fun onDestroy() {
        Exponea.unregisterSegmentationDataCallback(discoverySegmentsCallback)
        Exponea.unregisterSegmentationDataCallback(contentSegmentsCallback)
        Exponea.unregisterSegmentationDataCallback(merchandisingSegmentsCallback)
        super.onDestroy()
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
