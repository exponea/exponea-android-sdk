package com.exponea.sdk.manager

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import com.exponea.sdk.Exponea
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.manager.TrackingConsentManager.MODE.CONSIDER_CONSENT
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.TokenFrequency
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.network.ExponeaServiceImpl
import com.exponea.sdk.network.NetworkHandlerImpl
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.repository.PushNotificationRepository
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepository
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockTrackingDelegateImpl
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.TokenType
import com.exponea.sdk.util.runWithTimeout

/**
 * Purpose of TimeLimitedFcmManager is to mirror a implementation of FcmManagerImpl but without SDK usage.
 * TimeLimitedFcmManager is used in case of SDK API usage, when SDK is not initialized yet and we still need to handle
 * FCM data (notifications, push tokens)
 * !!! Keep behavior in sync with FcmManagerImpl
 * !!! Dont use ExponeaSDK API nor internal parts here
 * !!! All implementations must be limited by total time (max 10s) due to Android OS limitations and usage
 */
internal class TimeLimitedFcmManagerImpl : FcmManagerImpl {

    companion object {

        private val NOTIF_BITMAP_DOWNLOAD_TIMELIMIT: Long = 5000
        private val FLUSH_TIMELIMIT: Long = 5000

        /**
         * Creates an instance of TimeLimitedFcmManager that is intependent from SDK initialization process.
         */
        fun createSdklessInstance(context: Context, configuration: ExponeaConfiguration): TimeLimitedFcmManagerImpl {
            val preferences = ExponeaPreferencesImpl(context)
            val eventRepository = TemporaryEventRepositoryImpl(context, preferences)
            val uniqueIdentifierRepository = UniqueIdentifierRepositoryImpl(preferences)
            val customerIdsRepository = CustomerIdsRepositoryImpl(
                ExponeaGson.instance, uniqueIdentifierRepository, preferences
            )
            val networkManager = NetworkHandlerImpl(configuration)
            val exponeaService = ExponeaServiceImpl(ExponeaGson.instance, networkManager)
            val connectionManager = ConnectionManagerImpl(context)
            val flushManager = TimeLimitedFlushManagerImpl(
                configuration,
                eventRepository,
                exponeaService,
                connectionManager,
                {
                    // no action for identifyCustomer - SDK is not initialized
                },
                FLUSH_TIMELIMIT
            )
            val projectFactory = try {
                ExponeaProjectFactory(context, configuration)
            } catch (e: InvalidConfigurationException) {
                if (configuration.advancedAuthEnabled) {
                    Logger.w(this, "Turning off advanced auth for notification data tracking")
                    configuration.advancedAuthEnabled = false
                }
                ExponeaProjectFactory(context, configuration)
            }
            val eventManager = EventManagerImpl(
                configuration, eventRepository, customerIdsRepository, flushManager, projectFactory,
                onEventCreated = { event, type ->
                    // no action for any event - SDK is not initialized
                }
            )
            val pushTokenRepository = PushTokenRepositoryProvider.get(context)
            val pushNotificationRepository = PushNotificationRepositoryImpl(preferences)
            val campaignRepository = CampaignRepositoryImpl(ExponeaGson.instance, preferences)
            val inappMessageTrackingDelegate = EventManagerInAppMessageTrackingDelegate(
                context, eventManager
            )
            val inAppContentBlockTrackingDelegate = InAppContentBlockTrackingDelegateImpl(
                context, eventManager
            )
            val trackingConsentManager = TrackingConsentManagerImpl(
                eventManager, campaignRepository, inappMessageTrackingDelegate, inAppContentBlockTrackingDelegate
            )
            return TimeLimitedFcmManagerImpl(
                context, configuration, eventManager, pushTokenRepository,
                pushNotificationRepository, trackingConsentManager
            )
        }
    }

    private val trackingConsentManager: TrackingConsentManager

    constructor(
        context: Context,
        configuration: ExponeaConfiguration,
        eventManager: EventManager,
        pushTokenRepository: PushTokenRepository,
        pushNotificationRepository: PushNotificationRepository,
        trackingConsentManager: TrackingConsentManager
    ) : super(
        context,
        configuration,
        eventManager,
        pushTokenRepository,
        pushNotificationRepository
    ) {
        this.trackingConsentManager = trackingConsentManager
    }

    override fun trackToken(token: String?, tokenTrackFrequency: TokenFrequency?, tokenType: TokenType?) {
        // keep flow with FcmManagerImpl, time limit will be applied in `TimeLimitedEventManagerImpl`
        super.trackToken(token, tokenTrackFrequency, tokenType)
    }

    override fun onSelfCheckReceived() {
        // self check has no meaning while usage of this manager
        // but there is chance that SDK may be initialized meanwhile
        if (Exponea.isInitialized) {
            super.onSelfCheckReceived()
        } else {
            Logger.w(this, "Self-check notification has been delivered but not handled")
        }
    }

    override fun trackDeliveredPush(payload: NotificationPayload, deliveredTimestamp: Double) {
        if (payload.notificationData.hasTrackingConsent) {
            trackingConsentManager.trackDeliveredPush(
                payload.notificationData, deliveredTimestamp, CONSIDER_CONSENT
            )
        } else {
            Logger.i(this, "Event for delivered notification is not tracked because consent is not given")
        }
    }

    override fun showNotification(manager: NotificationManager, payload: NotificationPayload) {
        // keep flow with FcmManagerImpl, time limit will be applied in `getBitmapFromUrl`
        super.showNotification(manager, payload)
    }

    /**
     * Tries to download Bitmap for notification. Image may be too large or slow network.
     * If Image cannot be download in limited time, no image is returned.
     */
    override fun getBitmapFromUrl(url: String): Bitmap? {
        return runWithTimeout(NOTIF_BITMAP_DOWNLOAD_TIMELIMIT, {
            super.getBitmapFromUrl(url)
        }, {
            Logger.w(this, "Bitmap download takes too long")
            null
        })
    }

    override fun handleRemoteMessage(
        messageData: Map<String, String>?,
        manager: NotificationManager,
        showNotification: Boolean,
        timestamp: Double
    ) {
        // keep flow with FcmManagerImpl, time limit will be applied while tracking and showing of notification
        super.handleRemoteMessage(messageData, manager, showNotification, timestamp)
    }
}
