package com.exponea.sdk.models

import android.content.Context
import com.exponea.sdk.services.DefaultAppInboxProvider
import java.util.concurrent.TimeUnit

internal object Constants {
    // Network
    object Repository {
        val baseURL: String = "https://api.exponea.com"
    }

    // SDK Info
    object DeviceInfo {
        const val osName: String = "Android"
        const val sdk: String = "AndroidSDK"
    }

    // Type of customer events
    object EventTypes {
        val inbox: String = "app_inbox"
        val installation: String = "installation"
        val sessionEnd: String = "session_end"
        val sessionStart: String = "session_start"
        val payment: String = "payment"
        val push: String = "campaign"
        val banner: String = "banner"
        val pushTokenTrack: String = "notification_state"
    }

    // Default session values
    object Session {
        const val defaultTimeout = 60.0
        const val defaultAutomaticTracking = true
    }

    // General constants
    object General {
        val bannerFilename: String = "personalization"
        val bannerFilenameExt: String = "html"
        val bannerFullFilename: String = "personalization.html"
    }

    // Flush default setup
    object Flush {
        val defaultFlushMode = FlushMode.IMMEDIATE
        val defaultFlushPeriod = FlushPeriod(60, TimeUnit.MINUTES)
    }

    // Push notifications default setup
    object PushNotif {
        const val defaultAutomaticListening = true
        const val source = "xnpe_platform"
        const val fcmTokenProperty = "google_push_notification_id"
        const val hmsTokenProperty = "huawei_push_notification_id"
        const val fcmSelfCheckPlatformProperty = "android"
        const val hmsSelfCheckPlatformProperty = "huawei"
        const val defaultPushAuthorizationRequired = false
    }

    // Token default setup
    object Token {
        val defaultTokenFrequency = ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE
    }

    // Logger default setup
    object Logger {
        val defaultLoggerLevel = com.exponea.sdk.util.Logger.Level.INFO
    }

    // Campaign default setup
    object Campaign {
        const val defaultCampaignTTL = 10.0
    }

    // In-app messages
    object InApps {
        val defaultInAppMessageDelegate = object : InAppMessageCallback {
            override var overrideDefaultBehavior = false
            override var trackActions = true

            override fun inAppMessageShown(message: InAppMessage, context: Context) {
                // do nothing here as default
            }

            override fun inAppMessageError(message: InAppMessage?, errorMessage: String, context: Context) {
                // do nothing here as default
            }

            override fun inAppMessageClickAction(message: InAppMessage, button: InAppMessageButton, context: Context) {
                // do nothing here as default
            }

            override fun inAppMessageCloseAction(
                message: InAppMessage,
                button: InAppMessageButton?,
                interaction: Boolean,
                context: Context
            ) {
                // do nothing here as default
            }
        }
    }

    // AppInbox messages
    object AppInbox {
        val defaulAppInboxProvider = DefaultAppInboxProvider()
    }

    enum class PushNotifShownStatus(val value: String) {
        NOT_SHOWN("not_shown"),
        SHOWN("shown")
    }

    // Default values for applicationId
    object ApplicationId {
        const val APP_ID_DEFAULT_VALUE: String = "default-application"
        const val APP_ID_VALIDATION_REGEX: String = "^[a-z0-9]+(?:[-.][a-z0-9]+)*$"
        const val APP_ID_MAX_LENGTH: Int = 50
    }

    object PushPermissionStatus {
        const val PERMISSION_GRANTED = "Permission granted"
        const val PERMISSION_DENIED = "Permission denied"
        const val INVALIDATED_TOKEN = "Invalidated"
    }
}
