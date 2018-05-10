package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.*
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.models.FlushMode.PERIOD
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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
     * Check if our library has been properly initialized
     */

    internal var isInitialized: Boolean = false
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

        initializeSdk()
    }

    /**
     * Update the informed properties to a specific customer.
     * All properties will be stored into database until it will be
     * flushed (send it to api).
     */

    fun updateCustomerProperties(customerIds: CustomerIds, properties: PropertiesList) {
        trackEvent(
                customerId = customerIds,
                properties = properties.toHashMap(),
                route = Route.TRACK_CUSTOMERS
        )
    }

    /**
     * Track customer event add new events to a specific customer.
     * All events will be stored into database until it will be
     * flushed (send it to api).
     */

    fun trackCustomerEvent(
            customerIds: CustomerIds,
            properties: PropertiesList,
            timestamp: Long?,
            eventType: String?
    ) {
        trackEvent(
                customerId = customerIds,
                properties = properties.toHashMap(),
                timestamp = timestamp,
                eventType = eventType,
                route = Route.TRACK_EVENTS
        )
    }

    /**
     * Manually push all events to Exponea
     */

    fun flush() {
        if (component.flushManager.isRunning) {
            Logger.w(this, "Cannot flush, Job service is already in progress")
            return
        }

        component.flushManager.flush()
    }


    /**
     * Fetches customer attributes
     */
    fun fetchCustomerAttributes(customerIds: CustomerIds,
                                attributes: MutableList<HashMap<String, Any>>,
                                onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
                                onFailure: (String) -> Unit) {

       component.fetchManager.fetchCustomerAttributes(
               projectToken = configuration.projectToken,
               attributes = CustomerAttributes(customerIds, attributes),
               onSuccess = onSuccess,
               onFailure = onFailure
       )
    }


    fun fetchCustomerEvents(customerIds: CustomerIds,
                            eventTypes: ArrayList<String> = arrayListOf("page_view"),
                            order: String = "desc",
                            limit: Int = 3,
                            skip: Int = 100,
                            onFailure: (String) -> Unit,
                            onSuccess: (Result<ArrayList<CustomerEventModel>>) -> Unit
                            ) {
        component.fetchManager.fetchCustomerEvents(
                projectToken = configuration.projectToken,
                customerEvents = CustomerEvents(customerIds, eventTypes, order, limit, skip),
                onFailure = onFailure,
                onSuccess = onSuccess
        )
    }


    fun fetchRecommendation(customerIds: CustomerIds,
                            customerRecommendation: CustomerRecommendation,
                            onSuccess: (Result<List<CustomerAttributeModel>>) -> Unit,
                            onFailure: (String) -> Unit) {

        component.fetchManager.fetchCustomerAttributes(
                projectToken = configuration.projectToken,
                attributes = CustomerAttributes(customerIds, mutableListOf(customerRecommendation.toHashMap())),
                onSuccess = onSuccess,
                onFailure = onFailure
        )
    }

    /**
     * Manually track FCM Token to Exponea API
     */

    fun trackFcmToken(customerIds: CustomerIds, fcmToken: String) {
        val properties = PropertiesList(hashMapOf(Pair("push_notification_token", fcmToken)))
        updateCustomerProperties(customerIds, properties)
    }

    /**
     * Manually track delivered push notification to Exponea API
     */

    fun trackDeliveredPush(customerIds: CustomerIds, fcmToken: String, timestamp: Long? = null) {
        val properties: PropertiesList = PropertiesList(
                hashMapOf(
                        Pair("action_type", "notification"),
                        Pair("status", "delivered")
                )
        )
        Exponea.trackCustomerEvent(
                customerIds = customerIds,
                properties = properties,
                eventType = "campaign",
                timestamp = timestamp)
    }

    /**
     * Manually track clicked push notification to Exponea API
     */

    fun trackClickedPush(customerIds: CustomerIds, fcmToken: String, timestamp: Long? = null) {

        val properties: PropertiesList = PropertiesList(
                hashMapOf(
                        Pair("action_type", "notification"),
                        Pair("status", "clicked")
                )
        )

        Exponea.trackCustomerEvent(
                customerIds = customerIds,
                properties = properties,
                eventType = "campaign",
                timestamp = timestamp)
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
        trackInstall()

        // Track In-App purchase
        trackInAppPurchase()

    }

    private fun onFlushPeriodChanged() {
        Logger.d(this, "onFlushPeriodChanged: $flushPeriod")
        startService()
    }

    private fun onFlushModeChanged() {
        Logger.d(this, "onFlushModeChanged: $flushMode")
        when (flushMode) {
            PERIOD -> startService()
        // APP_CLOSE -> // TODO somehow implement this
            MANUAL -> stopService()
        }
    }

    private fun startService() {
        Logger.d(this, "startService")

        if (flushMode == MANUAL) {
            Logger.w(this, "Flush mode manual set -> Skipping job service")
            return
        }
        component.serviceManager.start()
    }

    private fun stopService() {
        Logger.d(this, "stopService")
        component.serviceManager.stop()
    }

    private fun trackInAppPurchase() {
        if (this.configuration.automaticSessionTracking) {
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

    internal fun trackEvent(
            eventType: String? = null,
            timestamp: Long? = Date().time,
            customerId: CustomerIds? = null,
            properties: HashMap<String, Any> = hashMapOf(),
            route: Route
    ) {

        if (!isInitialized) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }

        val event = ExportedEventType(
                type = eventType,
                timestamp = timestamp,
                customerIds = customerId,
                properties = properties
        )

        component.eventManager.addEventToQueue(event, route)
    }

    /**
     * Installation event is fired only once for the whole lifetime of the app on one
     * device when the app is launched for the first time.
     */

    internal fun trackInstall(
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

        trackEvent(
                eventType = "installation",
                properties = device.toHashMap(),
                route = Route.TRACK_EVENTS
        )

        component.deviceInitiatedRepository.set(true)
    }
}