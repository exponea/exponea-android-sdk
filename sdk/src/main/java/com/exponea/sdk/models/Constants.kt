package com.exponea.sdk.models

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
        val installation: String = "installation"
        val sessionEnd: String = "session_end"
        val sessionStart: String = "session_start"
        val payment: String = "payment"
        val push: String = "campaign"
        val banner: String = "banner"
    }

    // Default session values
    object Session {
        const val defaultTimeout = 20.0
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
}
