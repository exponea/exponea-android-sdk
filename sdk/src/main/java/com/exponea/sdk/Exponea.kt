package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.*
import com.exponea.sdk.models.FlushMode.*
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.addAppStateCallbacks
import com.google.firebase.FirebaseApp
import io.paperdb.Paper
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

@SuppressLint("StaticFieldLeak")
object Exponea {
    private lateinit var context: Context
    private lateinit var configuration: ExponeaConfiguration
    lateinit var component: ExponeaComponent

    /**
     * Defines which mode the library should flush out events
     */

    var flushMode: FlushMode = PERIOD
        set(value) {
            field = value
            onFlushModeChanged()
        }

    /**
     * Defines the period at which the library should flush events
     */

    var flushPeriod: FlushPeriod = FlushPeriod(60, TimeUnit.MINUTES)
        set(value) {
            field = value
            onFlushPeriodChanged()
        }

    /**
     * Defines session timeout considered for app usage
     */

    var sessionTimeout: Double
        get() = configuration.sessionTimeout
        set(value) {
            configuration.sessionTimeout = value
        }

    /**
     * Defines if automatic session tracking is enabled
     */
    var isAutomaticSessionTracking: Boolean
        get() = configuration.automaticSessionTracking
        set(value) {
            configuration.automaticSessionTracking = value
            startSessionTracking(value)
        }

    /**
     * Check if our library has been properly initialized
     */

    var isInitialized: Boolean = false
        get() {
            return this::configuration.isInitialized
        }

    /**
     * Check if the push notification listener is set to automatically
     */

    internal var isAutoPushNotification: Boolean = true
        get() {
            return configuration.automaticPushNotification
        }
    /**
     * Set which level the debugger should output log messages
     */
    var loggerLevel: Logger.Level
        get () = Logger.level
        set(value) {
            Logger.level = value
        }


    @Throws(InvalidConfigurationException::class)
    fun init(context: Context, configFile: String) {
        // Try to parse our file
        val configuration = Exponea.component.fileManager.getConfigurationFromFile(configFile)

        // If our file isn't null then try initiating normally
        if (configuration != null) {
            init(context, configuration)
        } else {
            throw InvalidConfigurationException()
        }
    }



    fun init(context: Context, configuration: ExponeaConfiguration) {
        Logger.i(this, "Init")

        Paper.init(context)

        this.context = context
        this.configuration = configuration

        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }
        ExponeaConfigRepository.set(context, configuration)
        FirebaseApp.initializeApp(context)
        initializeSdk()
    }


    /**
     * Update the informed properties to a specific customer.
     * All properties will be stored into database until it will be
     * flushed (send it to api).
     */

    fun identifyCustomer(customerIds: CustomerIds, properties: PropertiesList) {
        component.customerIdsRepository.set(customerIds)
        track(
                properties = properties.properties,
                type = EventType.TRACK_CUSTOMER
        )
    }

    /**
     * Track customer event add new events to a specific customer.
     * All events will be stored into database until it will be
     * flushed (send it to api).
     */

    fun trackEvent(
            properties: PropertiesList,
            timestamp: Long? = Date().time,
            eventType: String?
    ) {

        track(
                properties = properties.properties,
                timestamp = timestamp,
                eventType = eventType,
                type = EventType.TRACK_EVENT
        )
    }

    /**
     * Manually push all events to Exponea
     */

    fun flushData() {
        if (component.flushManager.isRunning) {
            Logger.w(this, "Cannot flush, Job service is already in progress")
            return
        }

        component.flushManager.flushData()
    }


    /**
     * Fetches customer attributes
     */
    fun fetchCustomerAttributes(
            customerAttributes: CustomerAttributes,
            onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    ) {
        val customer = Exponea.component.customerIdsRepository.get()
        component.fetchManager.fetchCustomerAttributes(
                projectToken = configuration.projectToken,
                attributes = customerAttributes.apply { customerIds = customer },
                onSuccess = onSuccess,
                onFailure = onFailure
        )
    }


    /**
     * Fetches banners web representation
     * @param onSuccess - success callback, when data is ready
     * @param onFailure - failure callback, in case of errors
     */
    fun getPersonalizationWebLayer(
            onSuccess: (Result<ArrayList<BannerResult>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit) {
        // TODO map banners id's
        val customerIds = Exponea.component.customerIdsRepository.get()
        Exponea.component.personalizationManager.getWebLayer(
                customerIds = customerIds,
                projectToken = Exponea.configuration.projectToken,
                onSuccess = onSuccess,
                onFailure = onFailure
        )
    }


    /**
     * Manually tracks session start
     * @param timestamp - determines session start time
     */
    fun trackSessionStart(timestamp: Long = Date().time) {
        if (isAutomaticSessionTracking) {
            Logger.w(Exponea.component.sessionManager,
                    "Can't manually track session, since automatic tracking is on ")
            return
        }
        component.sessionManager.trackSessionStart(timestamp)
    }

    /**
     * Manually tracks session end
     * @param timestamp - determines session end time
     */
    fun trackSessionEnd(timestamp: Long = Date().time) {

        if (isAutomaticSessionTracking) {
            Logger.w(Exponea.component.sessionManager,
                    "Can't manually track session, since automatic tracking is on ")
            return
        }

        component.sessionManager.trackSessionEnd(timestamp)
    }


    /**
     * Fetch events for a specific customer.
     * @param customerEvents - Event from a specific customer to be tracked.
     * @param onFailure - Method will be called if there was an error.
     * @param onSuccess - this method will be called when data is ready.
     */
    fun fetchCustomerEvents(
            customerEvents: FetchEventsRequest,
            onFailure: (Result<FetchError>) -> Unit,
            onSuccess: (Result<ArrayList<CustomerEvent>>) -> Unit
    ) {
        val customer = Exponea.component.customerIdsRepository.get()
        customerEvents.customerIds = customer

        component.fetchManager.fetchCustomerEvents(
                projectToken = configuration.projectToken,
                customerEvents = customerEvents,
                onFailure = onFailure,
                onSuccess = onSuccess
        )
    }

    /**
     * Fetch recommendations for a specific customer.
     * @param customerRecommendation - Recommendation for the customer.
     * @param onFailure - Method will be called if there was an error.
     * @param onSuccess - this method will be called when data is ready.
     */
    fun fetchRecommendation(
            customerRecommendation: CustomerRecommendation,
            onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
            onFailure: (Result<FetchError>) -> Unit
    ) {
        val customer = Exponea.component.customerIdsRepository.get()
        component.fetchManager.fetchCustomerAttributes(
                projectToken = configuration.projectToken,
                attributes = CustomerAttributes(
                        customer,
                        mutableListOf(customerRecommendation.toHashMap())),
                onSuccess = onSuccess,
                onFailure = onFailure
        )
    }

    /**
     * Manually track FCM Token to Exponea API.
     */

    fun trackPushToken(fcmToken: String) {
        val properties = PropertiesList(hashMapOf("google_push_notification_id" to fcmToken))
        track(
                eventType = Constants.EventTypes.push,
                properties = properties.properties,
                type = EventType.PUSH_TOKEN
        )
    }

    /**
     * Manually track delivered push notification to Exponea API.
     */

    fun trackDeliveredPush(
            data: NotificationData? = null,
            fcmToken: String,
            timestamp: Long? = null) {
        val properties = PropertiesList(
                hashMapOf(
                        Pair("action_type", "notification"),
                        Pair("status", "delivered")
                )
        )
        data?.let {
            properties["campaign_id"] = it.campaignId
            properties["campaign_name"] = it.campaignName
            properties["action_id"] = it.actionId
        }
        track(
                eventType = Constants.EventTypes.push,
                properties = properties.properties,
                type = EventType.PUSH_DELIVERED

        )
    }

    /**
     * Manually track clicked push notification to Exponea API.
     */

    fun trackClickedPush(
            data: NotificationData? = null,
            fcmToken: String,
            timestamp: Long? = null
            ) {
        val properties = PropertiesList(
                hashMapOf(
                        "action_type" to "notification",
                        "status" to "clicked"
                )
        )

        data?.let {
            properties["campaign_id"] = data.campaignId
            properties["campaign_name"] = data.campaignName
            properties["action_id"] = data.actionId
        }
        track(
                eventType = Constants.EventTypes.push,
                properties = properties.properties,
                type = EventType.PUSH_OPENED

        )
    }


    /**
     * Opens a WebView showing the personalized page with the
     * banners for a specific customer.
     */

    fun showBanners(customerIds: CustomerIds) {
        Exponea.component.personalizationManager.showBanner(
                projectToken = Exponea.configuration.projectToken,
                customerIds = customerIds
        )
    }

    /**
     * Tracks payment manually
     * @param purchasedItem - represents payment details.
     * @param timestamp - Time in timestamp format where the event was created.
     */

    fun trackPaymentEvent(
            timestamp: Long = Date().time,
            purchasedItem: PurchasedItem
    ) {

        track(
                eventType = Constants.EventTypes.payment,
                timestamp = timestamp,
                properties = purchasedItem.toHashMap(),
                type = EventType.PAYMENT
        )
    }

    // Private Helpers

    /**
     * Initialize and start all services and automatic configurations.
     */

    private fun initializeSdk() {


        // Start Network Manager
        this.component = ExponeaComponent(this.configuration, context)

        // Alarm Manager Starter
        startService()

        // Track Install Event
        trackInstallEvent()

        // Track In-App purchase
        trackInAppPurchase()

        // Initialize session observer
        configuration.automaticSessionTracking = component
                .preferences.getBoolean(
                SessionManagerImpl
                        .PREF_SESSION_AUTO_TRACK, true
        )
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

                    }
                }
        )
    }



    /**
     * Start the service when the flush period was changed.
     */

    private fun onFlushPeriodChanged() {
        Logger.d(this, "onFlushPeriodChanged: $flushPeriod")
        startService()
    }

    /**
     * Start or stop the service when the flush mode was changed.
     */

    private fun onFlushModeChanged() {
        Logger.d(this, "onFlushModeChanged: $flushMode")
        when (flushMode) {
            PERIOD -> startService()
            APP_CLOSE -> stopService()
            MANUAL -> stopService()
        }
    }

    /**
     * Starts the service.
     */

    private fun startService() {
        Logger.d(this, "startService")

        if (flushMode == MANUAL) {
            Logger.w(this, "Flush mode manual set -> Skipping job service")
            return
        }
        component.serviceManager.start()
    }

    /**
     * Stops the service.
     */

    private fun stopService() {
        Logger.d(this, "stopService")
        component.serviceManager.stop()
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
     * Initializes payments listener
     */

    private fun trackInAppPurchase() {
        if (this.configuration.automaticPaymentTracking) {
            // Add the observers when the automatic session tracking is true.
            this.component.iapManager.configure()
            this.component.iapManager.startObservingPayments()
        } else {
            // Remove the observers when the automatic session tracking is false.
            this.component.iapManager.stopObservingPayments()
        }
    }


    /**
     * Send a tracking event to Exponea
     */

    internal fun track(
            eventType: String? = null,
            timestamp: Long? = Date().time,
            properties: HashMap<String, Any> = hashMapOf(),
            type: EventType
    ) {

        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }

        val customerIds = component.customerIdsRepository.get()

        val event = ExportedEventType(
                type = eventType,
                timestamp = null,
                customerIds = customerIds.toHashMap(),
                properties = properties
        )



        component.eventManager.addEventToQueue(event, type)
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

        val device = DeviceProperties(
                campaign = campaign,
                campaignId = campaignId,
                link = link,
                deviceType = component.deviceManager.getDeviceType()
        )

        track(
                eventType = Constants.EventTypes.installation,
                properties = device.toHashMap(),
                type = EventType.INSTALL
        )

        component.deviceInitiatedRepository.set(true)
    }
}