package com.exponea.example.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.exponea.example.R
import com.exponea.example.databinding.ActivityMainBinding
import com.exponea.example.services.ExampleAppInboxProvider
import com.exponea.example.view.NavigationItem.Anonymize
import com.exponea.example.view.NavigationItem.Fetch
import com.exponea.example.view.NavigationItem.InAppContentBlock
import com.exponea.example.view.NavigationItem.Manual
import com.exponea.example.view.NavigationItem.Track
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

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbar)
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

        if (!Exponea.isInitialized) {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
            return
        }

        setupNavigation()

        val deeplinkDestination = resolveDeeplinkDestination(intent)

        if (deeplinkDestination != null) {
            handleDeeplinkDestination(deeplinkDestination)
        } else if (savedInstanceState == null) {
            navigateToItem(NavigationItem.Fetch)
        }

        Exponea.registerSegmentationDataCallback(discoverySegmentsCallback)
        Exponea.registerSegmentationDataCallback(contentSegmentsCallback)
        Exponea.registerSegmentationDataCallback(merchandisingSegmentsCallback)
    }

    @SuppressLint("MissingSuperCall")
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

    private fun navigateToItem(item: NavigationItem) {
        viewBinding.navigation.selectedItemId = item.navigationId
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
            DeeplinkDestination.Anonymize -> navigateToItem(Anonymize)
            DeeplinkDestination.Fetch -> navigateToItem(Fetch)
            DeeplinkDestination.Manual -> navigateToItem(Manual)
            DeeplinkDestination.Track -> navigateToItem(Track)
            DeeplinkDestination.InAppCb -> navigateToItem(InAppContentBlock)
        }
    }

    private fun setupNavigation() {
        val navController = getNavController()
        val topLevelDestinationIds = setOf(
            R.id.fetchFragment,
            R.id.trackFragment,
            R.id.flushFragment,
            R.id.anonymizeFragment,
            R.id.inAppContentBlocksFragment
        )
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = topLevelDestinationIds
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        viewBinding.navigation.setupWithNavController(navController)
        onBackPressedDispatcher.addCallback {
            if (topLevelDestinationIds.contains(navController.currentDestination?.id)) {
                finish()
            } else if (!navController.navigateUp()) {
                finish()
            }
        }
    }

    private fun getNavController(): NavController {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = getNavController()
        return navController.navigateUp() || super.onSupportNavigateUp()
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

    internal fun openCarousel() {
        getNavController().navigate(NavigationItem.InAppCarousel.navigationId)
    }
}
