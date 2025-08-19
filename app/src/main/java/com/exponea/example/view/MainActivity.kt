package com.exponea.example.view

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PushNotificationDelegate
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isResumedActivity
import com.exponea.sdk.util.isViewUrlIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

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

        Exponea.inAppMessageActionCallback = getInAppMessageCallback()
        Exponea.appInboxProvider = ExampleAppInboxProvider()

        if (!Exponea.isInitialized) {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
            return
        }

        setupNavigation()
        Exponea.registerSegmentationDataCallback(discoverySegmentsCallback)
        Exponea.registerSegmentationDataCallback(contentSegmentsCallback)
        Exponea.registerSegmentationDataCallback(merchandisingSegmentsCallback)

        val deeplinkDestination = resolveDeeplinkDestination(intent)
        if (deeplinkDestination != null) {
            handleDeeplinkDestination(deeplinkDestination)
        } else if (savedInstanceState == null) {
            navigateToItem(NavigationItem.Fetch)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Exponea.handleCampaignIntent(intent, applicationContext)
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

    private fun resolveDeeplinkDestination(intent: Intent?): DeeplinkFlow? {
        fun String.toDeeplinkDestination() = when {
            this.contains("track") -> DeeplinkFlow.Track
            this.contains("flush") -> DeeplinkFlow.Manual
            this.contains("fetch") -> DeeplinkFlow.Fetch
            this.contains("inappcb") -> DeeplinkFlow.InAppCb
            this.contains("anonymize") -> DeeplinkFlow.Anonymize
            this.contains("stopAndContinue") -> DeeplinkFlow.StopAndContinue
            this.contains("stopAndRestart") -> DeeplinkFlow.StopAndRestart
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

    private fun handleDeeplinkDestination(deeplinkDestination: DeeplinkFlow) {
        when (deeplinkDestination) {
            DeeplinkFlow.Anonymize -> navigateToItem(Anonymize)
            DeeplinkFlow.Fetch -> navigateToItem(Fetch)
            DeeplinkFlow.Manual -> navigateToItem(Manual)
            DeeplinkFlow.Track -> navigateToItem(Track)
            DeeplinkFlow.InAppCb -> navigateToItem(InAppContentBlock)
            DeeplinkFlow.StopAndContinue -> {
                Exponea.stopIntegration()
                if (viewBinding.navigation.selectedItemId == 0) {
                    navigateToItem(Fetch)
                }
            }
            DeeplinkFlow.StopAndRestart -> {
                Exponea.stopIntegration()
                startActivity(Intent(this, AuthenticationActivity::class.java))
                finish()
            }
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
            override var trackActions = true

            override fun inAppMessageShown(message: InAppMessage, context: Context) {
                Logger.i(this, "In app message ${message.name} has been shown")
                if (message.name.contains("StopSDK")) {
                    Logger.i(this, "In app message ${message.name} will stop SDK")
                    CoroutineScope(Dispatchers.Default).async {
                        delay(4000)
                        Logger.i(this, "Stopping SDK")
                        Exponea.stopIntegration()
                    }
                }
            }

            override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {
                Logger.e(this, "Error occurred '$errorMessage' while showing in app message ${message?.name}")
            }

            override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {
                Logger.i(this, "In app message ${message.name} has been clicked: ${button.url}")
                if (messageIsForGdpr(message)) {
                    handleGdprUserResponse(button)
                } else if (button.url != null) {
                    openUrl(button)
                }
            }

            private fun handleGdprUserResponse(button: InAppMessageButton) {
                when (button.url) {
                    "https://bloomreach.com/tracking/allow" -> {
                        Exponea.trackEvent(
                            eventType = "gdpr",
                            properties = PropertiesList(hashMapOf(
                                "status" to "allowed"
                            ))
                        )
                    }
                    "https://bloomreach.com/tracking/deny" -> {
                        Logger.i(this, "Stopping SDK")
                        Exponea.stopIntegration()
                    }
                }
            }

            private fun messageIsForGdpr(message: InAppMessage): Boolean {
                // apply your detection for GDPR related In-app
                // our example app is triggering GDPR In-app by custom event tracking so we used it for detection
                // you may implement detection against message title, ID, payload, etc.
                return message.applyEventFilter("event_name", mapOf("property" to "gdpr"), null)
            }

            private fun openUrl(button: InAppMessageButton) {
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            data = Uri.parse(button.url)
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    Logger.e(this, "Unable to open URL", e)
                }
            }

            override fun inAppMessageCloseAction(
                message: InAppMessage,
                button: InAppMessageButton?,
                interaction: Boolean,
                context: Context
            ) {
                Logger.i(this, "In app message ${message.name} has been closed: ${button?.url}")
                if (messageIsForGdpr(message) && interaction) {
                    // regardless from `button` nullability, parameter `interaction` tells that user closed message
                    Logger.i(this, "Stopping SDK")
                    Exponea.stopIntegration()
                }
            }
        }
    }

    internal fun openCarousel() {
        getNavController().navigate(NavigationItem.InAppCarousel.navigationId)
    }
}
