package com.exponea.example.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exponea.example.config.InAppMessageHandler
import com.exponea.example.config.PushNotificationEvent
import com.exponea.example.config.PushNotificationHandler
import com.exponea.example.logging.LogEntry
import com.exponea.example.logging.MemoryLogger
import com.exponea.example.model.SdkSetupState
import com.exponea.example.navigation.DeeplinkFlow
import com.exponea.example.navigation.DeeplinkResolver
import com.exponea.example.services.ExampleAppInboxProvider
import com.exponea.example.status.SdkStatusStore
import com.exponea.example.ui.auth.AuthActivity
import com.exponea.example.ui.components.ResultPanel
import com.exponea.example.ui.logs.LogsBottomSheet
import com.exponea.example.ui.navigation.Routes
import com.exponea.example.ui.theme.BloomreachTheme
import com.exponea.example.ui.theme.BrBlack
import com.exponea.example.ui.theme.BrGrey600
import com.exponea.example.ui.theme.BrWhite
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isResumedActivity

class MainActivity : ComponentActivity() {

    private val tag = this::class.simpleName
    private var showLogs by mutableStateOf(false)
    private var pushDialog by mutableStateOf<PushNotificationEvent?>(null)
    private var deeplinkRoute by mutableStateOf<String?>(null)

    private val segmentationCallbacks = listOf(
        object : SegmentationDataCallback() {
            override val exposingCategory = "discovery"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                val message = "Segments: New for discovery with IDs: $segments"
                Log.i(tag, message)
                MemoryLogger.INSTANCE.append(LogEntry.Level.INFO, "Segmentation", message)
            }
        },
        object : SegmentationDataCallback() {
            override val exposingCategory = "content"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                val message = "Segments: New for content with IDs: $segments"
                Log.i(tag, message)
                MemoryLogger.INSTANCE.append(LogEntry.Level.INFO, "Segmentation", message)
            }
        },
        object : SegmentationDataCallback() {
            override val exposingCategory = "merchandising"
            override val includeFirstLoad = false
            override fun onNewData(segments: List<Segment>) {
                val message = "Segments: New for merchandising with IDs: $segments"
                Log.i(tag, message)
                MemoryLogger.INSTANCE.append(LogEntry.Level.INFO, "Segmentation", message)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Exponea.isInitialized) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setupSdkIntegrations()
        SdkStatusStore.syncFromSdk()
        handleDeeplinkIntent(intent)

        setContent {
            BloomreachTheme {
                MainScreen(
                    deeplinkRoute = deeplinkRoute,
                    onDeeplinkConsumed = { deeplinkRoute = null },
                    onShowLogs = { showLogs = true }
                )
                if (showLogs) {
                    LogsBottomSheet(onDismiss = { showLogs = false })
                }
                pushDialog?.let { event ->
                    AlertDialog(
                        onDismissRequest = { pushDialog = null },
                        containerColor = BrWhite,
                        titleContentColor = BrBlack,
                        textContentColor = BrGrey600,
                        title = { Text(event.title, fontWeight = FontWeight.Bold) },
                        text = {
                            ResultPanel(
                                text = event.message,
                                scrollable = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { pushDialog = null }) {
                                Text("OK", fontWeight = FontWeight.Bold, color = BrBlack)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Exponea.handleCampaignIntent(intent, applicationContext)
        handleDeeplinkIntent(intent)
    }

    override fun onDestroy() {
        segmentationCallbacks.forEach { Exponea.unregisterSegmentationDataCallback(it) }
        super.onDestroy()
    }

    private fun setupSdkIntegrations() {
        Exponea.loggerLevel = Logger.Level.VERBOSE
        Exponea.checkPushSetup = true
        Exponea.handleCampaignIntent(intent, applicationContext)
        Exponea.pushNotificationsDelegate = PushNotificationHandler.create { event ->
            if (isResumedActivity()) {
                pushDialog = event
            } else {
                Log.i(tag, event.message)
            }
        }
        Exponea.inAppMessageActionCallback = InAppMessageHandler.create(this)
        Exponea.appInboxProvider = ExampleAppInboxProvider()
        segmentationCallbacks.forEach { Exponea.registerSegmentationDataCallback(it) }
    }

    private fun handleDeeplinkIntent(intent: Intent?) {
        when (val flow = DeeplinkResolver.resolve(intent)) {
            DeeplinkFlow.StopAndContinue -> {
                stopIntegration()
                deeplinkRoute = Routes.FETCHING
            }
            DeeplinkFlow.StopAndRestart -> {
                stopIntegration()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            null -> Unit
            else -> deeplinkRoute = DeeplinkResolver.toRoute(flow)
        }
    }

    private fun stopIntegration() {
        Exponea.stopIntegration {
            SdkSetupState.reset()
            SdkStatusStore.syncFromSdk()
        }
    }
}
