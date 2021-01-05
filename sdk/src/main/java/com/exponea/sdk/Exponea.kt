package com.exponea.sdk

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Looper
import androidx.work.Configuration
import androidx.work.WorkManager
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.manager.ConfigurationFileManager
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
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.FlushMode.APP_CLOSE
import com.exponea.sdk.models.FlushMode.IMMEDIATE
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.models.FlushMode.PERIOD
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem
import com.exponea.sdk.models.Result
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.addAppStateCallbacks
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.isViewUrlIntent
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.returnOnException
import com.exponea.sdk.view.InAppMessagePresenter
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("StaticFieldLeak")
object Exponea {

    private lateinit var application: Application
    private lateinit var configuration: ExponeaConfiguration
    private lateinit var component: ExponeaComponent
    internal var telemetry: TelemetryManager? = null

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
    var notificationDataCallback: ((data: Map<String, String>) -> Unit)? = null
        set(value) = runCatching {
            requireInitialized {
                field = value
                val storeData = component.pushNotificationRepository.getExtraData()
                if (storeData != null) {
                    field?.invoke(storeData)
                    component.pushNotificationRepository.clearExtraData()
                }
            }
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
        }.logOnException()

    /**
     * Any exception in SDK will be logged and swallowed if flag is enabled, otherwise
     * the exception will be rethrown.
     * If we have application context and the application is debuggable, then safe mode is `disabled`.
     * Default is `enabled` - for any call to SDK method before init is called.
     * If we don't know if the app is build for debugging/release, `enabled` is the safest.
     * You can also set the value yourself that will override the default behaviour.
     */
    private var safeModeOverride: Boolean? = null
    internal var safeModeEnabled: Boolean
        get() {
            safeModeOverride?.let { return it }
            return if (this::application.isInitialized) {
                application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0
            } else {
                Logger.w(this, "No context available, defaulting to enabled safe mode")
                true
            }
        }
        set(value) {
            safeModeOverride = value
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
     * It's also called when the SDK is auto-initialized
     */
    @Synchronized fun init(context: Context, configuration: ExponeaConfiguration) = runCatching {
        this.application = context.applicationContext as Application
        if (isInitialized) {
            Logger.e(this, "Exponea SDK is already initialized!")
            return
        }

        configuration.validate()

        Logger.i(this, "Initializing Exponea SDK version ${BuildConfig.VERSION_NAME}")

        if (Looper.myLooper() == null)
            Looper.prepare()

        telemetry = TelemetryManager(context.applicationContext as Application)
        telemetry?.start()
        telemetry?.reportInitEvent(configuration)

        this.configuration = configuration
        ExponeaConfigRepository.set(context, configuration)
        initializeSdk(context)
        isInitialized = true
    }.run {
        val exception = exceptionOrNull()
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
        requireInitialized {
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
        requireInitialized {
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
        requireInitialized(notInitializedBlock = {
            onFlushFinished?.invoke(kotlin.Result.failure(Exception("Exponea SDK was not initialized properly!")))
        }) {
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
        requireInitialized {
            component.fetchManager.fetchConsents(
                exponeaProject = configuration.mainExponeaProject,
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
        requireInitialized {
            val customer = component.customerIdsRepository.get()
            component.fetchManager.fetchRecommendation(
                exponeaProject = configuration.mainExponeaProject,
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
        requireInitialized {
            if (isAutomaticSessionTracking) {
                Logger.w(
                    component.sessionManager,
                    "Can't manually track session, since automatic tracking is on "
                )
                return@requireInitialized
            }
            component.sessionManager.trackSessionStart(timestamp)
        }
    }.logOnException()

    /**
     * Manually tracks session end
     * @param timestamp - determines session end time ( in seconds )
     */
    fun trackSessionEnd(timestamp: Double = currentTimeSeconds()) = runCatching {
        requireInitialized {
            if (isAutomaticSessionTracking) {
                Logger.w(
                    component.sessionManager,
                    "Can't manually track session, since automatic tracking is on "
                )
                return@requireInitialized
            }

            component.sessionManager.trackSessionEnd(timestamp)
        }
    }.logOnException()

    // Called by background ExponeaSessionEndWorker
    internal fun trackAutomaticSessionEnd() = runCatching {
        component.sessionManager.trackSessionEnd()
    }.logOnException()

    /**
     * Manually track FCM Token to Exponea API.
     */

    fun trackPushToken(fcmToken: String) = runCatching {
        trackPushToken(
            fcmToken,
            ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH // always track it when tracking manually
        )
    }.logOnException()

    internal fun trackPushToken(
        fcmToken: String,
        tokenTrackFrequency: ExponeaConfiguration.TokenFrequency
    ) = runCatching {
        requireInitialized {
            component.fcmManager.trackFcmToken(fcmToken, tokenTrackFrequency)
        }
    }.logOnException()

    /**
     * Manually track delivered push notification to Exponea API.
     */

    fun trackDeliveredPush(
        data: NotificationData? = null,
        timestamp: Double? = currentTimeSeconds()
    ) = runCatching {
        requireInitialized {
            val properties = PropertiesList(
                hashMapOf("status" to "delivered", "platform" to "android")
            )
            data?.getTrackingData()?.forEach { properties[it.key] = it.value }
            component.eventManager.track(
                eventType = if (data?.hasCustomEventType == true) data.eventType else Constants.EventTypes.push,
                properties = properties.properties,
                type = if (data?.hasCustomEventType == true) EventType.TRACK_EVENT else EventType.PUSH_DELIVERED,
                timestamp = timestamp
            )
        }
    }.logOnException()

    /**
     * Manually track clicked push notification to Exponea API.
     */

    fun trackClickedPush(
        data: NotificationData? = null,
        actionData: NotificationAction? = null,
        timestamp: Double? = currentTimeSeconds()
    ) = runCatching {
        requireInitialized {
            val properties = PropertiesList(
                hashMapOf(
                    "status" to "clicked",
                    "platform" to "android",
                    "url" to (actionData?.url ?: "app"),
                    "cta" to (actionData?.actionName ?: "notification")
                )
            )
            if (data != null) {
                // we'll consider the campaign data as just created - for expiration handling
                data.campaignData.createdAt = currentTimeSeconds()
                component.campaignRepository.set(data.campaignData)
            }
            data?.getTrackingData()?.forEach { properties[it.key] = it.value }
            component.eventManager.track(
                eventType = if (data?.hasCustomEventType == true) data.eventType else Constants.EventTypes.push,
                properties = properties.properties,
                type = if (data?.hasCustomEventType == true) EventType.TRACK_EVENT else EventType.PUSH_OPENED,
                timestamp = timestamp
            )
        }
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
        requireInitialized {
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

    fun isExponeaPushNotification(message: RemoteMessage?): Boolean {
        if (message == null) return false
        return message.data["source"] == "xnpe_platform"
    }

    /**
     * Handles Exponea notification payload from FirebaseMessagingService.
     * Does not handle non-Exponea notifications, just returns false for them so you can process them yourself.
     * @param applicationContext application context required to auto-initialize ExponeaSDK
     * @param message the RemoteMessage payload received from Firebase
     * @param manager the system notification manager instance
     * @param showNotification indicates if the SDK should display the notification or just track it
     *
     * @return true if notification is coming from Exponea servers, false otherwise.
     */
    fun handleRemoteMessage(
        applicationContext: Context,
        message: RemoteMessage?,
        manager: NotificationManager,
        showNotification: Boolean = true
    ): Boolean = runCatching {
        if (!isExponeaPushNotification(message)) return@runCatching false

        autoInitialize(applicationContext) {
            component.fcmManager.handleRemoteMessage(message, manager, showNotification)
        }
        return true
    }.returnOnException { true }

    /**
     * Handles the notification payload from FirebaseMessagingService
     * @param message the RemoteMessage payload received from Firebase
     * @param manager the system notification manager instance
     * @param showNotification indicates if the SDK should display the notification or just track it
     */
    @Deprecated(
        message = "When app is not running we need to autoinitialize the sdk.",
        replaceWith = ReplaceWith(
            expression = "Exponea.handleRemoteMessage(applicationContext, message, manager, showNotification)"
        )
    )
    fun handleRemoteMessage(
        message: RemoteMessage?,
        manager: NotificationManager,
        showNotification: Boolean = true
    ) = runCatching {
        requireInitialized {
            component.fcmManager.handleRemoteMessage(message, manager, showNotification)
        }
    }.logOnException()

    internal fun <T> requireInitialized(notInitializedBlock: (() -> T)? = null, initializedBlock: () -> T): T? {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return notInitializedBlock?.invoke()
        }
        return initializedBlock()
    }

    // extra function without return type for Unit, above method would have return type Unit?
    internal fun requireInitialized(notInitializedBlock: (() -> Unit)? = null, initializedBlock: () -> Unit) {
        requireInitialized<Unit>(notInitializedBlock, initializedBlock)
    }

    internal fun <T> autoInitialize(
        applicationContext: Context,
        notInitializedBlock: (() -> T)? = null,
        initializedBlock: () -> T
    ): T? {
        if (!isInitialized) {
            val config = ExponeaConfigRepository.get(applicationContext)
            if (config == null) {
                if (notInitializedBlock == null) { // only log this if we don't have fallback
                    Logger.e(this, "Unable to automatically initialize Exponea SDK!")
                }
                return notInitializedBlock?.invoke()
            }
            init(applicationContext, config)
        }
        return requireInitialized(initializedBlock = initializedBlock)
    }

    internal fun autoInitialize(
        applicationContext: Context,
        notInitializedBlock: (() -> Unit)? = null,
        initializedBlock: () -> Unit
    ) {
        autoInitialize<Unit>(applicationContext, notInitializedBlock, initializedBlock)
    }

    // Private Helpers

    /**
     * Initialize and start all services and automatic configurations.
     */

    private fun initializeSdk(context: Context) {
        this.component = ExponeaComponent(this.configuration, context)

        telemetry?.reportEvent(
            com.exponea.sdk.telemetry.model.EventType.EVENT_COUNT,
            hashMapOf("count" to component.eventRepository.count().toString())
        )

        initWorkManager(context)

        if (flushMode == PERIOD) startPeriodicFlushService()

        trackInstallEvent()

        trackFirebaseToken()

        startSessionTracking(configuration.automaticSessionTracking)

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
                    Exponea.component.flushManager.flushData()
                }
            }
        )

        if (checkPushSetup && context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            component.pushNotificationSelfCheckManager.start()
        }
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
    private fun trackFirebaseToken() {
        if (isAutoPushNotification) {
            this.component.fcmManager.trackFcmToken(component.firebaseTokenRepository.get(), tokenTrackFrequency)
        }
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

    fun anonymize(
        exponeaProject: ExponeaProject = configuration.mainExponeaProject,
        projectRouteMap: Map<EventType, List<ExponeaProject>> = configuration.projectRouteMap
    ) = runCatching {
        requireInitialized {
            component.anonymize(exponeaProject, projectRouteMap)
            telemetry?.reportEvent(com.exponea.sdk.telemetry.model.EventType.ANONYMIZE)
        }
    }.logOnException()

    /**
     * Tries to handle Intent from Activity. If Intent contains data as defined for Deeplinks,
     * given Uri is parsed, info is send to Campaign server and TRUE is returned. Otherwise FALSE
     * is returned.
     */
    fun handleCampaignIntent(intent: Intent?, appContext: Context): Boolean = runCatching {
        return autoInitialize<Boolean>(appContext) {
            if (!intent.isViewUrlIntent("http")) {
                return@autoInitialize false
            }
            val campaignData = CampaignData(intent!!.data!!)
            if (!campaignData.isValid()) {
                Logger.w(this, "Intent doesn't contain a valid Campaign info in Uri: ${intent.data}")
                return@autoInitialize false
            }
            component.campaignRepository.set(campaignData)
            val properties = hashMapOf(
                "platform" to "Android",
                "timestamp" to campaignData.createdAt
            )
            properties.putAll(campaignData.getTrackingData())
            component.eventManager.track(
                eventType = Constants.EventTypes.push,
                properties = properties,
                type = EventType.CAMPAIGN_CLICK
            )
            return@autoInitialize true
        } ?: false
    }.returnOnException { false }

    // used by InAppMessageActivity to get currently displayed message
    internal val presentedInAppMessage: InAppMessagePresenter.PresentedMessage?
        get() {
            if (!isInitialized) return null
            return component.inAppMessagePresenter.presentedMessage
        }

    internal fun selfCheckPushReceived() {
        component.pushNotificationSelfCheckManager.selfCheckPushReceived()
    }
}
