package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.models.*
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.models.FlushMode.PERIOD
import com.exponea.sdk.util.FileManager
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object Exponea {
    private lateinit var context: Context
    internal lateinit var configuration: ExponeaConfiguration
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
     * Set which level the debugger should output log messages
     */

    var loggerLevel: Logger.Level
        get () = Logger.level
        set(value) {
            Logger.level = value
        }

    fun init(context: Context, configuration: ExponeaConfiguration?, configFile: String?) {
        Logger.i(this, "Init")

        Paper.init(context)

        this.context = context

        if (configuration == null && configFile == null) {
            Logger.e(this, "Please inform at least one kind of configuration")
            return
        }

        if (configuration != null) {
            this.configuration = configuration
        } else {
            val config = configFile?.let { FileManager.getConfigurationOfFile(it) }
            if (config != null) {
                this.configuration = config
            }
        }

        if (isInitialized == false) {
            Logger.e(this, "Exponea SDK was not initialized properly!")
            return
        }

        // Start Network Manager
        this.component = ExponeaComponent(this.configuration, context)

        // Alarm Manager Starter
        startService()

        // Track Install Event
        trackInstall()

        // Track In-App purchase
        trackInAppPurchase()
    }

    /**
     * Send a tracking event to Exponea
     */

    fun trackEvent(
            eventType: String? = null,
            timestamp: Long? = Date().time,
            customerId: CustomerIds? = null,
            properties: HashMap<String, Any> = hashMapOf(),
            route: Route
    ) {

        if (isInitialized == false) {
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
     * Update the informed properties to a specific customer.
     * All properties will be stored into coredata until it will be
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
     * All events will be stored into coredata until it will be
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

    // Private Helpers

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
     * Installation event is fired only once for the whole lifetime of the app on one
     * device when the app is launched for the first time.
     */

    internal fun trackInstall(campaign: String? = null, campaignId: String? = null, link: String? = null) {
        val hasInstalled = component.deviceInitiatedRepository.get()

        if (hasInstalled) {
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