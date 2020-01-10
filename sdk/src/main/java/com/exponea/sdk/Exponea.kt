package com.exponea.sdk

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.work.Configuration
import androidx.work.WorkManager
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.manager.ConfigurationFileManager
import com.exponea.sdk.models.BannerResult
import com.exponea.sdk.models.CampaignClickInfo
import com.exponea.sdk.models.Consent
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
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
import com.exponea.sdk.util.isDeeplinkIntent
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.returnOnException
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.RemoteMessage
import io.paperdb.Paper

@SuppressLint("StaticFieldLeak")
object Exponea {

    private lateinit var context: Context
    private lateinit var configuration: ExponeaConfiguration
    internal lateinit var component: ExponeaComponent
    internal var telemetry: TelemetryManager? = null

    /**
     * Defines which mode the library should flush out events
     */
    var flushMode: FlushMode = Constants.Flush.defaultFlushMode
        set(value) = runCatching {
            field = value
            onFlushModeChanged()
        }.logOnException()

    /**
     * Defines the period at which the library should flush events
     */
    var flushPeriod: FlushPeriod = Constants.Flush.defaultFlushPeriod
        set(value) = runCatching {
            field = value
            onFlushPeriodChanged()
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
            if (!isInitialized) {
                Logger.e(this, "SDK not initialized")
                return
            }
            field = value
            val storeData = component.pushNotificationRepository.getExtraData()
            if (storeData != null) {
                field?.invoke(storeData)
                component.pushNotificationRepository.clearExtraData()
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
     * Any exception in SDK will be logged and swallowed if flag is enabled, otherwise
     *  the exception will be rethrown
     */
    internal var safeModeEnabled = BuildConfig.safeModeEnabled
        set(value) = runCatching {
            field = value
        }.logOnException()

    /**
     * Use this method using a file as configuration. The SDK searches for a file called
     * "exponea_configuration.json" that must be inside the "assets" folder of your application
     */
    @Deprecated("use init(context) instead")
    @Throws(InvalidConfigurationException::class)
    fun initFromFile(context: Context) = runCatching {
        // Try to parse our file
        val configuration = ConfigurationFileManager.getConfigurationFromDefaultFile(context)

        // If our file isn't null then try initiating normally
        if (configuration != null) {
            init(context, configuration)
        } else {
            throw InvalidConfigurationException()
        }
    }.returnOnException { t ->
        // Due to backward compatibility, we have to rethrow exception for invalid configuration
        // Other exceptions are logged and swallowed
        if (t is InvalidConfigurationException) {
            throw t
        }
    }

    /**
     * Use this method using a file as configuration. The SDK searches for a file called
     * "exponea_configuration.json" that must be inside the "assets" folder of your application
     */
    fun init(context: Context): Boolean = runCatching {
        initFromFile(context)
        return@runCatching true
    }.returnOnException { false }

    fun init(context: Context, configuration: ExponeaConfiguration) = runCatching {
        if (isInitialized) {
            Logger.e(this, "Exponea SDK is already initialized!")
            return
        }

        Logger.i(this, "Init")

        if (Looper.myLooper() == null)
            Looper.prepare()

        telemetry = TelemetryManager(context)
        telemetry?.start()
        telemetry?.reportEvent("init", hashMapOf("sdk_version" to BuildConfig.VERSION_NAME))

        Paper.init(context)

        this.context = context
        this.configuration = configuration
        isInitialized = true
        ExponeaConfigRepository.set(context, configuration)
        FirebaseApp.initializeApp(context)
        initializeSdk()
    }.logOnException()

    /**
     * Update the informed properties to a specific customer.
     * All properties will be stored into database until it will be
     * flushed (send it to api).
     */

    fun identifyCustomer(customerIds: CustomerIds, properties: PropertiesList) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        component.customerIdsRepository.set(customerIds)
        component.eventManager.track(
            properties = properties.properties,
            type = EventType.TRACK_CUSTOMER
        )
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
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        component.eventManager.track(
            properties = properties.properties,
            timestamp = timestamp,
            eventType = eventType,
            type = EventType.TRACK_EVENT
        )
    }.logOnException()

    /**
     * Manually push all events to Exponea
     */

    fun flushData() = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        if (component.flushManager.isRunning) {
            Logger.w(this, "Cannot flush, Job service is already in progress")
            return
        }

        component.flushManager.flushData()
    }.logOnException()

    /**
     * Fetches banners web representation
     * @param onSuccess - success callback, when data is ready
     * @param onFailure - failure callback, in case of errors
     */
    fun getPersonalizationWebLayer(
        onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
        onFailure: (Result<FetchError>) -> Unit
    ) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        // TODO map banners id's
        val customerIds = component.customerIdsRepository.get()
        component.personalizationManager.getWebLayer(
            customerIds = customerIds,
            projectToken = configuration.projectToken,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
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
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        component.fetchManager.fetchConsents(
                projectToken = configuration.projectToken,
                onSuccess = onSuccess,
                onFailure = onFailure
        )
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
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        val customer = component.customerIdsRepository.get()
        component.fetchManager.fetchRecommendation(
            projectToken = configuration.projectToken,
            recommendationRequest = CustomerRecommendationRequest(
                customerIds = customer.toHashMap().filter { e -> e.value != null },
                options = recommendationOptions
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }.logOnException()

    /**
     * Manually tracks session start
     * @param timestamp - determines session start time ( in seconds )
     */
    fun trackSessionStart(timestamp: Double = currentTimeSeconds()) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        if (isAutomaticSessionTracking) {
            Logger.w(
                component.sessionManager,
                "Can't manually track session, since automatic tracking is on "
            )
            return
        }
        component.sessionManager.trackSessionStart(timestamp)
    }.logOnException()

    /**
     * Manually tracks session end
     * @param timestamp - determines session end time ( in seconds )
     */
    fun trackSessionEnd(timestamp: Double = currentTimeSeconds()) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        if (isAutomaticSessionTracking) {
            Logger.w(
                component.sessionManager,
                "Can't manually track session, since automatic tracking is on "
            )
            return
        }

        component.sessionManager.trackSessionEnd(timestamp)
    }.logOnException()

    /**
     * Manually track FCM Token to Exponea API.
     */

    fun trackPushToken(fcmToken: String) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        component.firebaseTokenRepository.set(fcmToken, System.currentTimeMillis())
        val properties = PropertiesList(hashMapOf("google_push_notification_id" to fcmToken))
        component.eventManager.track(
            eventType = Constants.EventTypes.push,
            properties = properties.properties,
            type = EventType.PUSH_TOKEN
        )
    }.logOnException()

    /**
     * Manually track delivered push notification to Exponea API.
     */

    fun trackDeliveredPush(
        data: NotificationData? = null,
        timestamp: Double? = currentTimeSeconds()
    ) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        val properties = PropertiesList(
            hashMapOf("status" to "delivered", "platform" to "android")
        )
        data?.getTrackingData()?.forEach { properties[it.key] = it.value }
        component.eventManager.track(
            eventType = if (data?.hasCustomEventType == true) data?.eventType else Constants.EventTypes.push,
            properties = properties.properties,
            type = if (data?.hasCustomEventType == true) EventType.TRACK_EVENT else EventType.PUSH_DELIVERED,
            timestamp = timestamp
        )
    }.logOnException()

    /**
     * Manually track clicked push notification to Exponea API.
     */

    fun trackClickedPush(
        data: NotificationData? = null,
        actionData: NotificationAction? = null,
        timestamp: Double? = currentTimeSeconds()
    ) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        val properties = PropertiesList(
            hashMapOf(
                "status" to "clicked",
                "platform" to "android",
                "url" to (actionData?.url ?: "app"),
                "cta" to (actionData?.actionName ?: "notification")
            )
        )
        data?.getTrackingData()?.forEach { properties[it.key] = it.value }
        component.eventManager.track(
            eventType = if (data?.hasCustomEventType == true) data?.eventType else Constants.EventTypes.push,
            properties = properties.properties,
            type = if (data?.hasCustomEventType == true) EventType.TRACK_EVENT else EventType.PUSH_OPENED,
            timestamp = timestamp
        )
    }.logOnException()

    /**
     * Opens a WebView showing the personalized page with the
     * banners for a specific customer.
     */

    fun showBanners(customerIds: CustomerIds) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        component.personalizationManager.showBanner(
            projectToken = configuration.projectToken,
            customerIds = customerIds
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
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        val properties = purchasedItem.toHashMap()
        properties.putAll(DeviceProperties(context).toHashMap())
        component.eventManager.track(
            eventType = Constants.EventTypes.payment,
            timestamp = timestamp,
            properties = properties,
            type = EventType.PAYMENT
        )
    }.logOnException()

    /**
     * Handles the notification payload from FirebaseMessagingService
     * @param message the RemoteMessage payload received from Firebase
     * @param manager the system notification manager instance
     * @param showNotification indicates if the SDK should display the notification or just track it
     */
    fun handleRemoteMessage(
        message: RemoteMessage?,
        manager: NotificationManager,
        showNotification: Boolean = true
    ) = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        component.fcmManager.handleRemoteMessage(message, manager, showNotification)
    }.logOnException()

    // Private Helpers

    /**
     * Initialize and start all services and automatic configurations.
     */

    private fun initializeSdk() {
        this.component = ExponeaComponent(this.configuration, context)

        initWorkManager()

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

        component.inAppMessageManager.preload()
    }

    /**
     * Initialize the WorkManager instance
     */
    private fun initWorkManager() {
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
        component.serviceManager.startPeriodicFlush(context, flushPeriod)
    }

    /**
     * Stops the periodic flush service.
     */

    private fun stopPeriodicFlushService() {
        Logger.d(this, "stopPeriodicFlushService")
        component.serviceManager.stopPeriodicFlush(context)
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
            this.component.fcmManager.trackFcmToken(component.firebaseTokenRepository.get())
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

        val properties = DeviceProperties(context).toHashMap()
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

    fun anonymize() = runCatching {
        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        component.anonymize()
    }.logOnException()

    /**
     * Tries to handle Intent from Activity. If Intent contains data as defined for Deeplinks,
     * given Uri is parsed, info is send to Campaign server and TRUE is returned. Otherwise FALSE
     * is returned.
     */
    fun handleCampaignIntent(intent: Intent?, appContext: Context): Boolean = runCatching {
        if (!isInitialized) {
            val config = ExponeaConfigRepository.get(appContext)
            if (config == null) {
                Logger.e(this, "Cannot track campaign intent, unable to automatically initialize Exponea SDK!")
                return false
            }
            Logger.d(this, "Newly initiated")
            init(appContext, config)
        }
        if (!intent.isDeeplinkIntent()) {
            return false
        }
        val event = CampaignClickInfo(intent!!.data!!)
        if (!event.isValid()) {
            Logger.w(this, "Intent doesn't contain a valid Campaign info in Uri: ${intent.data}")
            return false
        }
        component.campaignRepository.set(event)
        component.eventManager.track(
            eventType = Constants.EventTypes.push,
            properties = hashMapOf(
                    "timestamp" to event.createdAt,
                    "platform" to "Android",
                    "url" to event.completeUrl
            ),
            type = EventType.CAMPAIGN_CLICK
        )
        return true
    }.returnOnException { false }
}
