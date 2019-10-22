package com.exponea.sdk.models

import java.util.concurrent.TimeUnit

internal object Constants {
    /// Network
    object Repository {
        val baseURL: String = "https://api.exponea.com"
        val contentType: String = "application/json"
        val headerContentType: String = "content-type"
        val headerAccept: String = "accept"
        val headerContentLenght: String = "content-length"
        val headerAuthorization: String = "authorization"
    }

    /// Keys for plist files and userdefaults
    object Keys {
        val token: String = "exponeaProjectIdKey"
        val authorization: String = "exponeaAuthorization"
        val launchedBefore: String = "launchedBefore"
        val sessionStarted: String = "sessionStarted"
        val sessionEnded: String = "sessionEnded"
        val timeout: String = "sessionTimeout"
        val autoSessionTrack: String = "automaticSessionTrack"
        val appVersion: String = "CFBundleShortVersionString"
        val baseURL: String = "exponeaBaseURL"
    }

    /// SDK Info
    object DeviceInfo {
        val osName: String = "Android"
        val osVersion: String = "1.0.0" // get system version
        val sdk: String = "AndroidSDK"
        val sdkVersion: String = "1.0.0"
    }

    /// Type of customer events
    object EventTypes {
        val installation: String = "installation"
        val sessionEnd: String = "session_end"
        val sessionStart: String = "session_start"
        val payment: String = "payment"
        val push: String = "campaign"
    }

    /// Error messages
    object ErrorMessages {
        val tokenNotConfigured: String = "Project token is not configured. Please configure it before interact with the ExponeaSDK"
        val sdkNotConfigured: String = "ExponeaSDK isn't configured."
        val couldNotStartSession: String = "Could not start new session. Please verify the error log for more information"
        val couldNotEndSession: String = "Could not end session. Please verify the error log for more information"
        val couldNotTrackPayment: String = "The payment could not be tracked."
        val verifyLogError: String = "Please verify the error log for more information."
        val couldNotLoadReceipt: String = "Could not load the iTunes Store receipt"
    }

    /// Success messages
    object SuccessMessages {
        val sessionStarted: String = "Session succesfully started"
        val paymentDone: String = "Payment was succesfully tracked!"
    }

    /// Default session values
    object Session {
        const val defaultTimeout = 20.0
        const val defaultAutomaticTracking = true
    }

    /// General constants
    object General {
        val GooglePlay: String = "Google Play Store"
        val bannerFilename: String = "personalization"
        val bannerFilenameExt: String = "html"
        val bannerFullFilename: String = "personalization.html"
    }

    /// Flush default setup
    object Flush {
        val defaultFlushMode = FlushMode.IMMEDIATE
        val defaultFlushPeriod = FlushPeriod(60, TimeUnit.MINUTES)
    }

    /// Push notifications default setup
    object PushNotif {
        const val defaultAutomaticListening = true
    }

    /// Token default setup
    object Token {
        val defaultTokenFrequency = ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE
    }

    /// Logger default setup
    object Logger {
        val defaultLoggerLevel = com.exponea.sdk.util.Logger.Level.INFO
    }

    /// Campaign default setup
    object Campaign {
        const val defaultCampaignTTL = 10.0
    }
}
