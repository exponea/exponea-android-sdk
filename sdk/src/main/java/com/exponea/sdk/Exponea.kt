package com.exponea.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.work.Configuration
import androidx.work.WorkManager
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.manager.CampaignManager
import com.exponea.sdk.manager.CampaignManagerImpl
import com.exponea.sdk.manager.ConfigurationFileManager
import com.exponea.sdk.manager.FcmManager
import com.exponea.sdk.manager.TimeLimitedFcmManagerImpl
import com.exponea.sdk.manager.TrackingConsentManager
import com.exponea.sdk.manager.TrackingConsentManager.MODE.CONSIDER_CONSENT
import com.exponea.sdk.manager.TrackingConsentManager.MODE.IGNORE_CONSENT
import com.exponea.sdk.manager.TrackingConsentManagerImpl
import com.exponea.sdk.models.CampaignData
import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaConfiguration.TokenFrequency
import com.exponea.sdk.models.ExponeaNotificationActionType
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.FlushMode.APP_CLOSE
import com.exponea.sdk.models.FlushMode.IMMEDIATE
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.models.FlushMode.PERIOD
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockAction
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageCallback
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.MessageItemAction
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem
import com.exponea.sdk.models.PushNotificationDelegate
import com.exponea.sdk.models.PushOpenedData
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.receiver.NotificationsPermissionReceiver
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.PushNotificationRepository
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.services.AppInboxProvider
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.services.ExponeaInitManager
import com.exponea.sdk.services.MessagingUtils
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselViewController.Companion.DEFAULT_MAX_MESSAGES_COUNT
import com.exponea.sdk.services.inappcontentblock.ContentBlockCarouselViewController.Companion.DEFAULT_SCROLL_DELAY
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.OnForegroundStateListener
import com.exponea.sdk.util.TokenType
import com.exponea.sdk.util.VersionChecker
import com.exponea.sdk.util.addAppStateCallbacks
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.handleClickedPushUpdate
import com.exponea.sdk.util.handleReceivedPushUpdate
import com.exponea.sdk.util.isViewUrlIntent
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.logOnExceptionWithResult
import com.exponea.sdk.util.returnOnException
import com.exponea.sdk.util.runOnMainThread
import com.exponea.sdk.view.ContentBlockCarouselView
import com.exponea.sdk.view.InAppContentBlockPlaceholderView
import com.exponea.sdk.view.InAppMessagePresenter
import com.exponea.sdk.view.InAppMessageView
import java.util.concurrent.CopyOnWriteArrayList

@SuppressLint("StaticFieldLeak")
object Exponea {

    private lateinit var application: Application
    private lateinit var configuration: ExponeaConfiguration
    private lateinit var component: ExponeaComponent
    internal var telemetry: TelemetryManager? = null
    internal val initGate = ExponeaInitManager()

    /**
     * Cookie of the current customer. Null before the SDK is initialized
     */
    val customerCookie: String?
        get() = runCatching {
            return if (isInitialized) component.customerIdsRepository.get().cookie else null
        }.returnOnException {
            null
        }

    /**
     * Defines which mode the library should flush out events
     */
    var flushMode: FlushMode = Constants.Flush.defaultFlushMode
        set(value) = runCatching {
            field = value
            if (isInitialized) onFlushModeChanged()
        }.logOnException()

    /**
     * Defines the period at which the library should flush events
     */
    var flushPeriod: FlushPeriod = Constants.Flush.defaultFlushPeriod
        set(value) = runCatching {
            field = value
            if (isInitialized) onFlushPeriodChanged()
        }.logOnException()

    /**
     * Defines session timeout considered for app usage
     */
    var sessionTimeout: Double
        get() = runCatching {
            configuration.sessionTimeout
        }.returnOnException {
            Constants.Session.defaultTimeout
        }
        set(value) = runCatching {
            configuration.sessionTimeout = value
            ExponeaConfigRepository.set(application, configuration)
        }.logOnException()

    /**
     * Defines if automatic session tracking is enabled
     */
    var isAutomaticSessionTracking: Boolean
        get() = runCatching {
            configuration.automaticSessionTracking
        }.returnOnException {
            Constants.Session.defaultAutomaticTracking
        }
        set(value) = runCatching {
            configuration.automaticSessionTracking = value
            ExponeaConfigRepository.set(application, configuration)
            startSessionTracking(value)
        }.logOnException()

    /**
     * Check if our library has been properly initialized
     */
    var isInitialized: Boolean = false
        internal set

    /**
     * Check if the push notification listener is set to automatically
     */
    var isAutoPushNotification: Boolean
        get() = runCatching {
            configuration.automaticPushNotification
        }.returnOnException {
            Constants.PushNotif.defaultAutomaticListening
        }
        set(value) = runCatching {
            configuration.automaticPushNotification = value
            ExponeaConfigRepository.set(application, configuration)
        }.logOnException()

    /**
     * Indicate the frequency which Firebase token needs to be updated
     */
    val tokenTrackFrequency: ExponeaConfiguration.TokenFrequency
        get() = runCatching {
            configuration.tokenTrackFrequency
        }.returnOnException {
            Constants.Token.defaultTokenFrequency
        }

    /**
     * Whenever a notification with extra values is received, this callback is called
     * with the values as map
     *
     * If a previous data was received and no listener was attached to the callback,
     * that data i'll be dispatched as soon as a listener is attached
     */
    @Deprecated("Use pushNotificationDelegate instead")
    var notificationDataCallback: ((data: Map<String, Any>) -> Unit)? = null
        set(value) = runCatching {
            initGate.waitForInitialize {
                field = value
                val storeData = component.pushNotificationRepository.getExtraData()
                if (storeData != null) {
                    field?.invoke(storeData)
                    component.pushNotificationRepository.clearExtraData()
                }
            }
        }.logOnException()

    /**
     * Whenever a notification is received or clicked, this callback is called
     * with the values.
     *
     * If a notification is received or clicked and no listener was attached to the callback,
     * that data will be dispatched as soon as a listener is attached.
     */
    var pushNotificationsDelegate: PushNotificationDelegate? = null
        set(value) = runCatching {
            field = value
            if (value == null) {
                return@runCatching
            }
            val repository = getPushNotificationRepository()
            repository?.popDeliveredPushData()?.forEach {
                value.handleReceivedPushUpdate(it)
            }
            repository?.popClickedPushData()?.forEach {
                value.handleClickedPushUpdate(it)
            }
        }.logOnException()

    /**
     * Whenever a in-app message button is clicked, this callback is called, if set up.
     * Otherwise default button behaviour is handled by the SDK
     */
    var inAppMessageActionCallback: InAppMessageCallback = Constants.InApps.defaultInAppMessageDelegate
        set(value) = runCatching {
            field = value
        }.logOnException()

    /**
     * Set which level the debugger should output log messages
     */
    var loggerLevel: Logger.Level
        get() = runCatching {
            Logger.level
        }.returnOnException {
            Constants.Logger.defaultLoggerLevel
        }
        set(value) = runCatching {
            Logger.level = value
        }.logOnException()

    /**
     * Defines time to live of campaign click event in seconds considered for app usage
     */
    var campaignTTL: Double
        get() = runCatching {
            configuration.campaignTTL
        }.returnOnException {
            Constants.Campaign.defaultCampaignTTL
        }
        set(value) = runCatching {
            configuration.campaignTTL = value
            ExponeaConfigRepository.set(application, configuration)
        }.logOnException()

    /**
     * Default properties to be tracked with all events.
     * Provide default properties in configuration, they're exposed here for run-time configuration.
     */
    var defaultProperties: HashMap<String, Any>
        get() = runCatching {
            configuration.defaultProperties
        }.returnOnException { hashMapOf() }
        set(value) = runCatching {
            configuration.defaultProperties = value
            ExponeaConfigRepository.set(application, configuration)
        }.logOnException()

    /**
     * Any exception in SDK will be logged and swallowed if flag is enabled, otherwise
     * the exception will be rethrown.
     * If we have application context and the app is debuggable or runDebugMode is TRUE, then safe mode is `disabled`.
     * Default is `enabled` - for any call to SDK method before init is called.
     * If we don't know if the app is build for debugging/release, `enabled` is the safest.
     * You can also set the value yourself that will override the default behaviour.
     */
    internal var safeModeOverride: Boolean? = null
    var safeModeEnabled: Boolean
        get() {
            safeModeOverride?.let { return it }
            return if (this::application.isInitialized) {
                !runDebugMode
            } else {
                Logger.w(this, "No context available, defaulting to enabled safe mode")
                true
            }
        }
        set(value) {
            safeModeOverride = value
        }

    /**
     * Tells to SDK if app is running in Debug mode.
     * In Debug mode, SDK is invoking VersionChecker step and turns off safeMode (saveMode could be overridden).
     */
    internal var runDebugModeOverride: Boolean? = null
    var runDebugMode: Boolean
        get() {
            runDebugModeOverride?.let { return it }
            return if (this::application.isInitialized) {
                application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            } else {
                Logger.w(this, "No context available, debug mode is false by default")
                false
            }
        }
        set(value) {
            runDebugModeOverride = value
        }

    /**
     * To help developers with integration, we can automatically check push notification setup
     * when application is started in debug mode.
     * When integrating push notifications(or when testing), we
     * advise you to turn this feature on before initializing the SDK.
     * Self-check only runs in debug mode and does not do anything in release builds.
     */
    var checkPushSetup: Boolean = false

    /**
     * Callback is notified for segmentation data updates.
     */
    internal var segmentationDataCallbacks = CopyOnWriteArrayList<SegmentationDataCallback>()

    /**
     * Registers callback to be notified for segmentation data updates.
     */
    fun registerSegmentationDataCallback(callback: SegmentationDataCallback) = runCatching {
        segmentationDataCallbacks.add(callback)
        getComponent()?.segmentsManager?.onCallbackAdded(callback)
        return@runCatching
    }.logOnException()

    /**
     * Unregisters callback from to be notified for segmentation data updates.
     * Removing of already unregistered callback does nothing.
     */
    fun unregisterSegmentationDataCallback(callback: SegmentationDataCallback) = runCatching {
        segmentationDataCallbacks.removeAll { it == callback }
        return@runCatching
    }.logOnException()

    /**
     * Use this method using a file as configuration. The SDK searches for a file called
     * "exponea_configuration.json" that must be inside the "assets" folder of your application
     */
    @Deprecated("Json configuration is deprecated")
    @Throws(InvalidConfigurationException::class)
    fun initFromFile(context: Context) = runCatching {
        // Try to parse our file
        val configuration = ConfigurationFileManager.getConfigurationFromDefaultFile(context)

        // If our file isn't null then try initiating normally
        if (configuration != null) {
            init(context, configuration)
        } else {
            throw InvalidConfigurationException("Unable to locate/initiate configuration")
        }
    }.returnOnException { t ->
        if (t is InvalidConfigurationException) {
            throw t
        }
    }

    /**
     * Use this method using a file as configuration. The SDK searches for a file called
     * "exponea_configuration.json" that must be inside the "assets" folder of your application
     */
    @Deprecated("Json configuration is deprecated")
    fun init(context: Context): Boolean = runCatching {
        @Suppress("DEPRECATION")
        initFromFile(context)
        return@runCatching true
    }.returnOnException { t ->
        if (t is InvalidConfigurationException) {
            throw t
        }
        false
    }

    /**
     * This is the main init method that should be called to initialize Exponea SDK
     */
    @Synchronized fun init(context: Context, configuration: ExponeaConfiguration) = runCatching {
        this.application = context.applicationContext as Application
        if (isInitialized) {
            Logger.e(this, "Exponea SDK is already initialized!")
            return
        }

        configuration.validate()

        Logger.i(this, "Initializing Exponea SDK version ${BuildConfig.EXPONEA_VERSION_NAME}")

        if (Looper.myLooper() == null)
            Looper.prepare()

        telemetry = TelemetryManager(context.applicationContext as Application)
        telemetry?.start()
        telemetry?.reportInitEvent(configuration)

        this.configuration = configuration
        ExponeaConfigRepository.set(context, configuration)
        initializeSdk(context)
        isInitialized = true
        initGate.notifyInitializedState()
        ExponeaContextProvider.registerForegroundStateListener(object : OnForegroundStateListener {
            override fun onStateChanged(isForeground: Boolean) {
                requireInitialized(
                    notInitializedBlock = {
                        Logger.w(this, "Exponea deinitialized meanwhile, stopping PushNotifPermission checker")
                        ExponeaContextProvider.removeForegroundStateListener(this)
                    },
                    initializedBlock = {
                        if (isForeground) {
                            trackSavedToken()
                        }
                    }
                )
            }
        })
    }.run {
        val exception = exceptionOrNull()
        if (exception != null) {
            initGate.notifyInitializedState()
        }
        if (exception is InvalidConfigurationException) {
            throw exception
        }
        this
    }.logOnException()

    /**
     * Update the informed properties to a specific customer.
     * All properties will be stored into database until it will be
     * flushed (send it to api).
     */

    fun identifyCustomer(customerIds: CustomerIds, properties: PropertiesList) = runCatching {
        initGate.waitForInitialize {
            component.customerIdsRepository.set(customerIds)
            component.eventManager.track(
                properties = properties.properties,
                type = EventType.TRACK_CUSTOMER
            )
        }
    }.logOnException()

    /**
     * Track customer event add new events to a specific customer.
     * All events will be stored into database until it will be
     * flushed (send it to api).
     */

    fun trackEvent(
        properties: PropertiesList,
        timestamp: Double? = currentTimeSeconds(),
        eventType: String?
    ) = runCatching {
        initGate.waitForInitialize {
            component.eventManager.track(
                properties = properties.properties,
                timestamp = timestamp,
                eventType = eventType,
                type = EventType.TRACK_EVENT
            )
        }
    }.logOnException()

    /**
     * Manually push all events to Exponea
     */

    fun flushData(onFlushFinished: ((kotlin.Result<Unit>) -> Unit)? = null) = runCatching {
        initGate.waitForInitialize {
            component.flushManager.flushData(onFlushFinished)
        }
    }.logOnException()

    /**
     * Fetch the list of your existing consent categories.
     * @param onSuccess - success callback, when data is ready
     * @param onFailure - failure callback, in case of errors
     */
    fun getConsents(
        onSuccess: (Result<ArrayList<Consent>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) = runCatching {
        initGate.waitForInitialize {
            component.fetchManager.fetchConsents(
                exponeaProject = component.projectFactory.mainExponeaProject,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            telemetry?.reportEvent(com.exponea.sdk.telemetry.model.EventType.FETCH_CONSENTS)
        }
    }.logOnException()

    /**
     * Fetch recommendations for a specific customer.
     * @param recommendationOptions - Recommendation options for the customer.
     * @param onFailure - Method will be called if there was an error.
     * @param onSuccess - this method will be called when data is ready.
     */
    fun fetchRecommendation(
        recommendationOptions: CustomerRecommendationOptions,
        onSuccess: (Result<ArrayList<CustomerRecommendation>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) = runCatching {
        initGate.waitForInitialize {
            val customer = component.customerIdsRepository.get()
            component.fetchManager.fetchRecommendation(
                exponeaProject = component.projectFactory.mainExponeaProject,
                recommendationRequest = CustomerRecommendationRequest(
                    customerIds = customer.toHashMap().filter { e -> e.value != null },
                    options = recommendationOptions
                ),
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            telemetry?.reportEvent(com.exponea.sdk.telemetry.model.EventType.FETCH_RECOMMENDATION)
        }
    }.logOnException()

    /**
     * Manually tracks session start
     * @param timestamp - determines session start time ( in seconds )
     */
    fun trackSessionStart(timestamp: Double = currentTimeSeconds()) = runCatching {
        initGate.waitForInitialize {
            if (isAutomaticSessionTracking) {
                Logger.w(
                    component.sessionManager,
                    "Can't manually track session, since automatic tracking is on "
                )
                return@waitForInitialize
            }
            component.sessionManager.trackSessionStart(timestamp)
        }
    }.logOnException()

    /**
     * Manually tracks session end
     * @param timestamp - determines session end time ( in seconds )
     */
    fun trackSessionEnd(timestamp: Double = currentTimeSeconds()) = runCatching {
        initGate.waitForInitialize {
            if (isAutomaticSessionTracking) {
                Logger.w(
                    component.sessionManager,
                    "Can't manually track session, since automatic tracking is on "
                )
                return@waitForInitialize
            }

            component.sessionManager.trackSessionEnd(timestamp)
        }
    }.logOnException()

    // Called by background ExponeaSessionEndWorker
    internal fun trackAutomaticSessionEnd() = runCatching {
        component.sessionManager.trackSessionEnd()
    }.logOnException()

    /**
     * Manually track Fcm Token to Exponea API.
     */
    fun trackPushToken(token: String) = runCatching {
        trackPushTokenInternal(
            token,
            ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, // always track it when tracking manually
            TokenType.FCM
        )
    }.logOnException()

    /**
     * Manually track Hms Token to Exponea API.
     */
    fun trackHmsPushToken(token: String) = runCatching {
        trackPushTokenInternal(
            token,
            ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, // always track it when tracking manually
            TokenType.HMS
        )
    }.logOnException()

    private fun trackPushTokenInternal(
        fcmToken: String,
        tokenTrackFrequency: ExponeaConfiguration.TokenFrequency,
        tokenType: TokenType
    ) = runCatching {
        initGate.waitForInitialize {
            component.fcmManager.trackToken(fcmToken, tokenTrackFrequency, tokenType)
        }
    }.logOnException()

    /**
     * Manually track delivered push notification to Exponea API.
     * Event is tracked even if NotificationData have not a tracking consent.
     */
    fun trackDeliveredPushWithoutTrackingConsent(
        data: NotificationData? = null,
        timestamp: Double = currentTimeSeconds()
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackDeliveredPush(
                data,
                timestamp,
                IGNORE_CONSENT,
                Constants.PushNotifShownStatus.SHOWN,
                component.fcmManager.findNotificationChannelImportance()
            )
        }
    }.logOnException()

    /**
     * Manually track delivered push notification to Exponea API.
     * Event is tracked if parameter 'data' has TRUE value of 'hasTrackingConsent' property
     */
    fun trackDeliveredPush(
        data: NotificationData? = null,
        timestamp: Double = currentTimeSeconds()
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackDeliveredPush(
                data,
                timestamp,
                CONSIDER_CONSENT,
                Constants.PushNotifShownStatus.SHOWN,
                component.fcmManager.findNotificationChannelImportance()
            )
        }
    }.logOnException()

    /**
     * Manually track clicked push notification to Exponea API.
     * Event is tracked if one or both conditions met:
     * - parameter 'data' has TRUE value of 'hasTrackingConsent' property
     * - parameter 'actionData' has TRUE value of 'isTrackingForced' property
     */
    fun trackClickedPush(
        data: NotificationData? = null,
        actionData: NotificationAction? = null,
        timestamp: Double? = currentTimeSeconds()
    ) = runCatching {
        val trackingConsentManager = getTrackingConsentManager()
        if (trackingConsentManager == null) {
            Logger.w(this, "Unable to start tracking flow, waiting for SDK init")
            initGate.waitForInitialize {
                component.trackingConsentManager.trackClickedPush(
                    data, actionData, timestamp, CONSIDER_CONSENT
                )
            }
            return@runCatching
        }
        trackingConsentManager.trackClickedPush(
            data, actionData, timestamp, CONSIDER_CONSENT
        )
    }.logOnException()

    /**
     * Manually track clicked push notification to Exponea API.
     * Event is tracked even if NotificationData and NotificationAction have not a tracking consent.
     */
    fun trackClickedPushWithoutTrackingConsent(
        data: NotificationData? = null,
        actionData: NotificationAction? = null,
        timestamp: Double? = currentTimeSeconds()
    ) = runCatching {
        val trackingConsentManager = getTrackingConsentManager()
        if (trackingConsentManager == null) {
            Logger.w(this, "Unable to start tracking flow, waiting for SDK init")
            initGate.waitForInitialize {
                component.trackingConsentManager.trackClickedPush(
                    data, actionData, timestamp, IGNORE_CONSENT
                )
            }
            return@runCatching
        }
        trackingConsentManager.trackClickedPush(
            data, actionData, timestamp, IGNORE_CONSENT
        )
    }.logOnException()

    /**
     * Tracks payment manually
     * @param timestamp - Time in timestamp format where the event was created. ( in seconds )
     * @param purchasedItem - represents payment details.
     */
    fun trackPaymentEvent(
        timestamp: Double = currentTimeSeconds(),
        purchasedItem: PurchasedItem
    ) = runCatching {
        initGate.waitForInitialize {
            val properties = purchasedItem.toHashMap()
            properties.putAll(DeviceProperties(application).toHashMap())
            component.eventManager.track(
                eventType = Constants.EventTypes.payment,
                timestamp = timestamp,
                properties = properties,
                type = EventType.PAYMENT
            )
        }
    }.logOnException()

    fun isExponeaPushNotification(messageData: Map<String, String>?): Boolean {
        if (messageData == null) return false
        return messageData["source"] == Constants.PushNotif.source
    }

    /**
     * Handles Exponea notification payload.
     * Does not handle non-Exponea notifications, just returns false for them so you can process them yourself.
     * @param applicationContext application context required to check notifications permission
     * @param messageData the message payload
     * @param manager the system notification manager instance
     * @param showNotification indicates if the SDK should display the notification or just track it
     *
     * @return true if notification is coming from Exponea servers, false otherwise.
     */
    fun handleRemoteMessage(
        applicationContext: Context,
        messageData: Map<String, String>?,
        manager: NotificationManager,
        showNotification: Boolean = true
    ): Boolean = runCatching {
        if (!isExponeaPushNotification(messageData)) return@runCatching false
        val fcmManagerInstance = getFcmManager(applicationContext)
        if (fcmManagerInstance != null) {
            fcmManagerInstance.handleRemoteMessage(messageData, manager, showNotification)
        }
        return true
    }.returnOnException { true }

    private fun getTrackingConsentManager(): TrackingConsentManager? {
        if (isInitialized) {
            return component.trackingConsentManager
        }
        val applicationContext = ExponeaContextProvider.applicationContext
        if (applicationContext == null) {
            Logger.e(this, "Tracking process cannot continue without known application context")
            return null
        }
        // TrackingConsentManager without SDK init
        try {
            return TrackingConsentManagerImpl.createSdklessInstance(applicationContext)
        } catch (e: Exception) {
            Logger.e(this, "Tracking not handled" +
                " error occured while preparing a tracking process, see logs", e)
            return null
        }
    }

    /**
     * Returns FcmManager implementation that fits current SDK state:
     * - SDK is initialized = FcmManager impl from SDK is returned
     * - SDK is not initialized = Lightweigt SimpleFcmManager is returned
     */
    private fun getFcmManager(applicationContext: Context): FcmManager? {
        if (isInitialized) {
            return component.fcmManager
        }
        // Simple FCM manager - without SDK init
        val configuration = ExponeaConfigRepository.get(applicationContext)
        if (configuration == null) {
            Logger.w(this, "Notification delivery not handled," +
                " previous SDK configuration not found")
            return null
        }
        try {
            return TimeLimitedFcmManagerImpl.createSdklessInstance(applicationContext, configuration)
        } catch (e: Exception) {
            Logger.e(this, "Notification delivery not handled," +
                " error occured while preparing a handling process, see logs", e)
            return null
        }
    }

    /**
     * Returns PushNotificationRepository implementation that fits current SDK state:
     * - SDK is initialized = PushNotificationRepository impl from SDK is returned
     * - SDK is not initialized = Lightweigt PushNotificationRepository is returned
     */
    internal fun getPushNotificationRepository(): PushNotificationRepository? {
        if (isInitialized) {
            return component.pushNotificationRepository
        }
        val applicationContext = ExponeaContextProvider.applicationContext
        if (applicationContext == null) {
            Logger.e(
                this,
                "Notification data not stored, application context not found"
            )
            return null
        }
        // Simple PushNotificationRepository - without SDK init
        try {
            return PushNotificationRepositoryImpl(ExponeaPreferencesImpl(applicationContext))
        } catch (e: Exception) {
            Logger.e(
                this,
                "Notification data not stored due to error, see logs", e)
            return null
        }
    }

    internal fun <T> requireInitialized(notInitializedBlock: (() -> T)? = null, initializedBlock: () -> T): T? {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return notInitializedBlock?.invoke()
        }
        return initializedBlock()
    }

    // extra function without return type for Unit, above method would have return type Unit?
    private fun requireInitialized(notInitializedBlock: (() -> Unit)? = null, initializedBlock: () -> Unit) {
        requireInitialized<Unit>(notInitializedBlock, initializedBlock)
    }

    // Private Helpers

    /**
     * Initialize and start all services and automatic configurations.
     */

    private fun initializeSdk(context: Context) {
        this.component = ExponeaComponent(this.configuration, context)

        component.eventRepository.tryToMigrateFromPaper()

        ensureOnBackgroundThread {
            telemetry?.reportEvent(
                com.exponea.sdk.telemetry.model.EventType.EVENT_COUNT,
                hashMapOf("count" to component.eventRepository.count().toString())
            )
        }

        initWorkManager(context)

        if (flushMode == PERIOD) startPeriodicFlushService()

        trackInstallEvent()

        trackSavedToken()

        startSessionTracking(configuration.automaticSessionTracking)

        component.inAppContentBlockManager.loadInAppContentBlockPlaceholders()

        component.segmentsManager.onSdkInit()

        context.addAppStateCallbacks(
            onOpen = {
                Logger.i(this, "App is opened")
                if (flushMode == APP_CLOSE) {
                    flushMode = PERIOD
                }
            },
            onClosed = {
                Logger.i(this, "App is closed")
                if (flushMode == PERIOD) {
                    flushMode = APP_CLOSE
                    // Flush data when app is closing for flush mode periodic.
                    component.flushManager.flushData()
                }
            }
        )

        if (checkPushSetup && runDebugMode) {
            component.pushNotificationSelfCheckManager.start()
        }
        if (runDebugMode && !isUnitTest()) {
            VersionChecker(component.networkManager, context).warnIfNotLatestSDKVersion()
        }
    }

    internal fun isUnitTest(): Boolean {
        val device = Build.DEVICE ?: ""
        val product = Build.PRODUCT ?: ""
        return device == "robolectric" && product == "robolectric"
    }

    /**
     * Initialize the WorkManager instance
     */
    private fun initWorkManager(context: Context) {
        try {
            WorkManager.initialize(context, Configuration.Builder().build())
        } catch (e: Exception) {
            Logger.i(this, "WorkManager already init, skipping")
        }
    }

    /**
     * Start the service when the flush period was changed.
     */

    private fun onFlushPeriodChanged() {
        Logger.d(this, "onFlushPeriodChanged: $flushPeriod")
        if (flushMode == PERIOD) startPeriodicFlushService()
    }

    /**
     * Start or stop the service when the flush mode was changed.
     */

    private fun onFlushModeChanged() {
        Logger.d(this, "onFlushModeChanged: $flushMode")
        when (flushMode) {
            PERIOD -> startPeriodicFlushService()
            APP_CLOSE -> stopPeriodicFlushService()
            MANUAL -> stopPeriodicFlushService()
            IMMEDIATE -> stopPeriodicFlushService()
        }
    }

    /**
     * Starts the periodic flush service.
     */

    private fun startPeriodicFlushService() {
        Logger.d(this, "startPeriodicFlushService")

        if (flushMode != PERIOD) {
            Logger.w(this, "Flush mode is not period -> Not starting periodic flush service")
            return
        }
        component.serviceManager.startPeriodicFlush(application, flushPeriod)
    }

    /**
     * Stops the periodic flush service.
     */

    private fun stopPeriodicFlushService() {
        Logger.d(this, "stopPeriodicFlushService")
        component.serviceManager.stopPeriodicFlush(application)
    }

    /**
     * Initializes session listener
     * @param enableSessionTracking - determines sdk tracking session's state
     */

    private fun startSessionTracking(enableSessionTracking: Boolean) {
        if (enableSessionTracking) {
            component.sessionManager.startSessionListener()
        } else {
            component.sessionManager.stopSessionListener()
        }
    }

    /**
     * Send the firebase token
     */
    private fun trackSavedToken() {
        this.component.fcmManager.trackToken(
            component.pushTokenRepository.get(),
            tokenTrackFrequency,
            component.pushTokenRepository.getLastTokenType())
    }

    /**
     * Installation event is fired only once for the whole lifetime of the app on one
     * device when the app is launched for the first time.
     */

    internal fun trackInstallEvent(
        campaign: String? = null,
        campaignId: String? = null,
        link: String? = null
    ) {

        if (component.deviceInitiatedRepository.get()) {
            return
        }

        val properties = DeviceProperties(application).toHashMap()
        campaign?.let { properties["campaign"] = it }
        campaignId?.let { properties["campaign_id"] = it }
        link?.let { properties["link"] = it }

        component.eventManager.track(
            eventType = Constants.EventTypes.installation,
            properties = properties,
            type = EventType.INSTALL
        )

        component.deviceInitiatedRepository.set(true)
    }

    /**
     * Xamarin SDK binding library does not support default value parameters
     * so this method accepts nulls and replaces them with the default value internally.
     * Please do not modify.
     */
    fun anonymize(
        exponeaProject: ExponeaProject? = null,
        projectRouteMap: Map<EventType, List<ExponeaProject>>? = null
    ) = runCatching {
        requireInitialized(
                notInitializedBlock = {
                    Logger.v(this@Exponea, "Exponea is not initialize")
                },
                initializedBlock = {
                    initGate.clear()
                    component.anonymize(
                            exponeaProject ?: component.projectFactory.mainExponeaProject,
                            projectRouteMap ?: configuration.projectRouteMap
                    )
                    telemetry?.reportEvent(com.exponea.sdk.telemetry.model.EventType.ANONYMIZE)
                }
        )
    }.logOnException()

    /**
     * Tries to handle Intent from Activity. If Intent contains data as defined for Deeplinks,
     * given Uri is parsed, info is send to Campaign server and TRUE is returned. Otherwise FALSE
     * is returned.
     */
    fun handleCampaignIntent(intent: Intent?, appContext: Context): Boolean = runCatching {
        if (!intent.isViewUrlIntent()) {
            return false
        }
        val campaignUri = intent?.data
        if (campaignUri == null) {
            Logger.v(this, "Campaign click not tracked because URL is missing")
            return false
        }
        val campaignData = CampaignData(campaignUri)
        return getCampaignManager(appContext)?.trackCampaignClick(campaignData) ?: false
    }.returnOnException { false }

    private fun getCampaignManager(applicationContext: Context): CampaignManager? {
        if (isInitialized) {
            return component.campaignManager
        }
        // Campaign  manager - without SDK init
        val configuration = ExponeaConfigRepository.get(applicationContext)
        if (configuration == null) {
            Logger.w(
                this,
                "Campaign click not handled, previous SDK configuration not found"
            )
            return null
        }
        try {
            return CampaignManagerImpl.createSdklessInstance(applicationContext, configuration)
        } catch (e: Exception) {
            Logger.e(
                this,
                "Campaign click not handled, error occurred while preparing a handling process, see logs",
                e
            )
            return null
        }
    }

    // used by InAppMessageActivity to get currently displayed message
    internal val presentedInAppMessage: InAppMessagePresenter.PresentedMessage?
        get() {
            return inAppMessagePresenter?.presentedMessage
        }

    internal val inAppMessagePresenter: InAppMessagePresenter?
        get() {
            if (!isInitialized) return null
            return component.inAppMessagePresenter
        }

    // used by InAppMessageActivity to get currently displayed message View
    // View is not kept as field but generating from scratch to avoid memory leaks
    internal fun getPresentedInAppMessageView(activity: Activity): InAppMessageView? {
        val presenting = presentedInAppMessage ?: return null
        val inappMessageView = inAppMessagePresenter?.getView(
                activity,
                presenting.messageType,
                presenting.payload,
                presenting.payloadHtml,
                presenting.timeout,
                { button ->
                    presenting.actionCallback(button)
                    activity.finish()
                },
                { userInteraction, cancelButton ->
                    presenting.dismissedCallback(userInteraction, cancelButton)
                    activity.finish()
                },
                { error: String ->
                    presenting.failedCallback(error)
                    activity.finish()
                }
        )
        if (inappMessageView == null) {
            presenting.failedCallback("Unable to present message")
        }
        return inappMessageView
    }

    internal fun selfCheckPushReceived() {
        component.pushNotificationSelfCheckManager.selfCheckPushReceived()
    }

    /**
     * Use this method to track push token, when new token is obtained in firebase messaging service
     */
    fun handleNewToken(context: Context, token: String) {
        handleNewTokenInternal(
            context,
            token,
            null, // track it according to SDK configuration
            TokenType.FCM
        )
    }

    /**
     * Use this method to track push token, when new token is obtained in huawei messaging service
     */
    fun handleNewHmsToken(context: Context, token: String) {
        handleNewTokenInternal(
            context,
            token,
            null, // track it according to SDK configuration
            TokenType.HMS
        )
    }

    private fun handleNewTokenInternal(
        context: Context,
        token: String,
        tokenFrequency: TokenFrequency?,
        tokenType: TokenType
    ) {
        runCatching {
            Logger.d(this, "Received push notification token")
            val fcmManagerInstance = getFcmManager(context)
            if (fcmManagerInstance == null) {
                Logger.d(this, "Token not refreshed: SDK is not initialized nor configured previously")
                val pushTokenRepository = PushTokenRepositoryProvider.get(context)
                pushTokenRepository.setUntrackedToken(
                    token,
                    tokenType,
                    NotificationsPermissionReceiver.isPermissionGranted(context)
                )
            } else {
                Logger.d(this, "Token refresh")
                fcmManagerInstance.trackToken(token, tokenFrequency, tokenType)
            }
        }.logOnException()
    }

    /**
     * Track in-app message banner click event
     * Event is tracked if one or both conditions met:
     * - parameter 'message' has TRUE value of 'hasTrackingConsent' property
     * - parameter 'buttonLink' has TRUE value of query parameter 'xnpe_force_track'
     */
    fun trackInAppMessageClick(
        message: InAppMessage,
        buttonText: String?,
        buttonLink: String?
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppMessageClick(
                message, buttonText, buttonLink, CONSIDER_CONSENT
            )
        }
    }.logOnException()

    /**
     * Track in-app message banner click event
     * Event is tracked even if InAppMessage and button link have not a tracking consent.
     */
    fun trackInAppMessageClickWithoutTrackingConsent(
        message: InAppMessage,
        buttonText: String?,
        buttonLink: String?
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppMessageClick(
                message, buttonText, buttonLink, IGNORE_CONSENT
            )
        }
    }.logOnException()

    /**
     * Track in-app message banner close event
     * Event is tracked if parameter 'message' has TRUE value of 'hasTrackingConsent' property
     */
    fun trackInAppMessageClose(
        message: InAppMessage,
        buttonText: String? = null,
        interaction: Boolean? = null
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppMessageClose(
                message, buttonText, interaction ?: true, CONSIDER_CONSENT
            )
        }
    }.logOnException()

    /**
     * Track in-app message banner close event
     * Event is tracked even if InAppMessage has not a tracking consent.
     */
    fun trackInAppMessageCloseWithoutTrackingConsent(
        message: InAppMessage,
        buttonText: String? = null,
        interaction: Boolean? = null
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppMessageClose(
                message, buttonText, interaction ?: true, IGNORE_CONSENT
            )
        }
    }.logOnException()

    fun getAppInboxButton(context: Context): Button? = runCatching<Button?> {
        requireInitialized<Button>(
            initializedBlock = {
                Exponea.appInboxProvider.getAppInboxButton(context)
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    fun getAppInboxListView(
        context: Context,
        onItemClicked: (MessageItem, Int) -> Unit
    ): View? = runCatching<View?> {
        requireInitialized<View>(
            initializedBlock = {
                Exponea.appInboxProvider.getAppInboxListView(context, onItemClicked = onItemClicked)
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    fun getAppInboxListFragment(context: Context): Fragment? = runCatching<Fragment?> {
        requireInitialized<Fragment>(
            initializedBlock = {
                Exponea.appInboxProvider.getAppInboxListFragment(context)
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    fun getAppInboxDetailFragment(context: Context, messageId: String): Fragment? = runCatching<Fragment?> {
        requireInitialized<Fragment>(
            initializedBlock = {
                Exponea.appInboxProvider.getAppInboxDetailFragment(context, messageId)
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    fun getAppInboxDetailView(context: Context, messageId: String): View? = runCatching<View?> {
        requireInitialized<View>(
            initializedBlock = {
                Exponea.appInboxProvider.getAppInboxDetailView(context, messageId)
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    public fun fetchAppInbox(callback: ((List<MessageItem>?) -> Unit)) = runCatching {
        initGate.waitForInitialize {
            component.appInboxManager.fetchAppInbox(callback)
            telemetry?.reportEvent(com.exponea.sdk.telemetry.model.EventType.TRACK_INBOX_FETCH)
        }
    }.logOnException()

    fun fetchAppInboxItem(messageId: String, callback: (MessageItem?) -> Unit) = runCatching {
        initGate.waitForInitialize {
            component.appInboxManager.fetchAppInboxItem(messageId, callback)
        }
    }.logOnException()

    fun trackAppInboxOpened(item: MessageItem) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackAppInboxOpened(item, CONSIDER_CONSENT)
        }
    }.logOnException()

    fun trackAppInboxOpenedWithoutTrackingConsent(item: MessageItem) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackAppInboxOpened(item, IGNORE_CONSENT)
        }
    }.logOnException()

    fun trackAppInboxClick(action: MessageItemAction, message: MessageItem) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackAppInboxClicked(
                message, action.title, action.url, CONSIDER_CONSENT
            )
        }
    }.logOnException()

    fun trackAppInboxClickWithoutTrackingConsent(action: MessageItemAction, message: MessageItem) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackAppInboxClicked(
                message, action.title, action.url, IGNORE_CONSENT
            )
        }
    }.logOnException()

    fun markAppInboxAsRead(message: MessageItem, callback: ((Boolean) -> Unit)?) = runCatching {
        initGate.waitForInitialize {
            component.appInboxManager.markMessageAsRead(message, callback)
        }
    }.logOnException()

    var appInboxProvider: AppInboxProvider = Constants.AppInbox.defaulAppInboxProvider
        set(value) = runCatching {
            field = value
        }.logOnException()

    internal fun getComponent(): ExponeaComponent? = runCatching<ExponeaComponent?> {
        requireInitialized<ExponeaComponent>(
            initializedBlock = {
                component
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    fun getInAppContentBlocksPlaceholder(
        placeholderId: String,
        context: Context,
        config: InAppContentBlockPlaceholderConfiguration? = null
    ): InAppContentBlockPlaceholderView? = runCatching<InAppContentBlockPlaceholderView?> {
        requireInitialized<InAppContentBlockPlaceholderView>(
            initializedBlock = {
                Exponea.component.inAppContentBlockManager.getPlaceholderView(
                    placeholderId,
                    context,
                    config ?: InAppContentBlockPlaceholderConfiguration()
                )
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    fun getInAppContentBlocksCarousel(
        context: Context,
        placeholderId: String,
        maxMessagesCount: Int? = null,
        scrollDelay: Int? = null
    ): ContentBlockCarouselView? = runCatching<ContentBlockCarouselView?> {
        requireInitialized<ContentBlockCarouselView>(
            initializedBlock = {
                ContentBlockCarouselView(
                        context,
                        placeholderId,
                        maxMessagesCount ?: DEFAULT_MAX_MESSAGES_COUNT,
                        scrollDelay ?: DEFAULT_SCROLL_DELAY
                )
            }
        )
    }.logOnExceptionWithResult().getOrNull()

    fun requestPushAuthorization(context: Context, listener: (Boolean) -> Unit) = runCatching {
        NotificationsPermissionReceiver.requestPushAuthorization(context) { granted ->
            trackSavedToken()
            runCatching {
                listener(granted)
            }.logOnException()
        }
    }.logOnException()

    fun trackInAppContentBlockClick(
        placeholderId: String,
        action: InAppContentBlockAction,
        message: InAppContentBlock
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockClick(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    buttonText = action.name,
                    buttonLink = action.url,
                    mode = CONSIDER_CONSENT
            )
        }
    }.logOnException()

    fun trackInAppContentBlockClickWithoutTrackingConsent(
        placeholderId: String,
        action: InAppContentBlockAction,
        message: InAppContentBlock
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockClick(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    buttonText = action.name,
                    buttonLink = action.url,
                    mode = IGNORE_CONSENT
            )
        }
    }.logOnException()

    fun trackInAppContentBlockClose(
        placeholderId: String,
        message: InAppContentBlock
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockClose(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    mode = CONSIDER_CONSENT
            )
        }
    }.logOnException()

    fun trackInAppContentBlockCloseWithoutTrackingConsent(
        placeholderId: String,
        message: InAppContentBlock
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockClose(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    mode = IGNORE_CONSENT
            )
        }
    }.logOnException()

    fun trackInAppContentBlockShown(
        placeholderId: String,
        message: InAppContentBlock
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockShown(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    mode = CONSIDER_CONSENT
            )
        }
    }.logOnException()

    fun trackInAppContentBlockShownWithoutTrackingConsent(
        placeholderId: String,
        message: InAppContentBlock
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockShown(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    mode = IGNORE_CONSENT
            )
        }
    }.logOnException()

    fun trackInAppContentBlockError(
        placeholderId: String,
        message: InAppContentBlock,
        errorMessage: String
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockError(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    error = errorMessage,
                    mode = CONSIDER_CONSENT
            )
        }
    }.logOnException()

    fun trackInAppContentBlockErrorWithoutTrackingConsent(
        placeholderId: String,
        message: InAppContentBlock,
        errorMessage: String
    ) = runCatching {
        initGate.waitForInitialize {
            component.trackingConsentManager.trackInAppContentBlockError(
                    placeholderId = placeholderId,
                    contentBlock = message,
                    error = errorMessage,
                    mode = IGNORE_CONSENT
            )
        }
    }.logOnException()

    internal fun getSegmentationDataCallbacks(categoryName: String): List<SegmentationDataCallback> {
        return segmentationDataCallbacks.filter { it.exposingCategory == categoryName }
    }

    /**
     * Retrieves segmentation data for given category once.
     */
    fun getSegments(
        exposingCategory: String,
        force: Boolean = false,
        successCallback: ((List<Segment>) -> Unit)
    ) = runCatching {
        initGate.waitForInitialize {
            component.segmentsManager.fetchSegmentsManually(
                category = exposingCategory,
                forceFetch = force
            ) { segments ->
                Logger.i(
                    this,
                    "Segments: Manual segmentation fetch for $exposingCategory has been done successfully"
                )
                runCatching {
                    successCallback.invoke(segments)
                }.logOnException()
            }
        }
    }.logOnException()

    internal fun processPushNotificationClickInternally(openedPushDataIntent: Intent) {
        val action = openedPushDataIntent.getSerializableExtra(ExponeaExtras.EXTRA_ACTION_INFO) as? NotificationAction?
        Logger.d(this, "Interaction: $action")
        val notifActionType = when (openedPushDataIntent.action) {
            ExponeaExtras.ACTION_DEEPLINK_CLICKED -> ExponeaNotificationActionType.DEEPLINK
            ExponeaExtras.ACTION_URL_CLICKED -> ExponeaNotificationActionType.BROWSER
            else -> ExponeaNotificationActionType.APP
        }
        val data = openedPushDataIntent.getParcelableExtra(ExponeaExtras.EXTRA_DATA) as NotificationData?
        val payloadRawData = openedPushDataIntent
            .getSerializableExtra(ExponeaExtras.EXTRA_CUSTOM_DATA) as? HashMap<String, String>
        val deliveredTimestamp = openedPushDataIntent.getDoubleExtra(ExponeaExtras.EXTRA_DELIVERED_TIMESTAMP, 0.0)
        val payload = payloadRawData?.let {
            NotificationPayload(it).apply {
                this.deliveredTimestamp = deliveredTimestamp
            }
        }
        val now = currentTimeSeconds()
        val clickedTimestamp: Double = if (now <= deliveredTimestamp) {
            deliveredTimestamp + 1
        } else {
            now
        }
        if (data?.hasTrackingConsent == true || action?.isTrackingForced == true) {
            trackClickedPush(
                data = data,
                actionData = action,
                timestamp = clickedTimestamp
            )
        } else {
            Logger.e(this,
                "Event for clicked notification is not tracked because consent is not given nor forced")
        }
        if (payload != null) {
            notifyCallbacksForNotificationClick(notifActionType, action?.url, payload)
        }
        // send also broadcast with this action, so client app can also react to push open event
        ExponeaContextProvider.applicationContext?.let { context: Context ->
            Intent().also { broadcastIntent ->
                broadcastIntent.action = openedPushDataIntent.action
                broadcastIntent.putExtra(ExponeaExtras.EXTRA_ACTION_INFO, action)
                broadcastIntent.putExtra(ExponeaExtras.EXTRA_DATA, data)
                broadcastIntent.putExtra(
                    ExponeaExtras.EXTRA_CUSTOM_DATA,
                    openedPushDataIntent.getSerializableExtra(ExponeaExtras.EXTRA_CUSTOM_DATA)
                )
                broadcastIntent.`package` = context.packageName
                PendingIntent.getBroadcast(
                    context,
                    0,
                    broadcastIntent,
                    MessagingUtils.getPendingIntentFlags()
                ).send()
            }
        }
    }

    internal fun notifyCallbacksForNotificationDelivery(messageData: NotificationPayload) {
        val pushNotificationRepository = getPushNotificationRepository()
        // older pushNotificationRepository
        messageData.attributes?.let {
            if (notificationDataCallback == null) {
                pushNotificationRepository?.setExtraData(it)
            } else {
                runOnMainThread {
                    notificationDataCallback?.invoke(it)
                }
            }
        }
        // new pushNotificationsDelegate
        val pushNotificationsDelegateLocal = pushNotificationsDelegate
        val pushNotificationData = messageData.rawData
        if (pushNotificationsDelegateLocal == null) {
            pushNotificationRepository?.appendDeliveredNotification(pushNotificationData)
        } else {
            pushNotificationsDelegateLocal.handleReceivedPushUpdate(pushNotificationData)
        }
    }

    private fun notifyCallbacksForNotificationClick(
        actionType: ExponeaNotificationActionType,
        actionUrl: String?,
        messageData: NotificationPayload
    ) {
        val pushNotificationRepository = getPushNotificationRepository()
        val pushNotificationsDelegateLocal = pushNotificationsDelegate
        val pushOpenedData = PushOpenedData(
            actionType = actionType,
            actionUrl = actionUrl,
            extraData = messageData.rawData
        )
        if (pushNotificationsDelegateLocal == null) {
            pushNotificationRepository?.appendClickedNotification(pushOpenedData)
        } else {
            pushNotificationsDelegateLocal.handleClickedPushUpdate(pushOpenedData)
        }
    }
}
