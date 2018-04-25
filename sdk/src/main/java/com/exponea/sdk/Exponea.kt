package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.models.*
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.models.FlushMode.PERIOD
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import java.util.*
import java.util.concurrent.TimeUnit

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
            onFlushPeriodChanged()
        }

    /**
     * Defines the period at which the library should flush events
     */

    var flushPeriod: FlushPeriod = FlushPeriod(60, TimeUnit.MINUTES)
        set(value) {
            field = value
            onFlushModeChanged()
        }

    /**
     * Check if our library has been properly initialized
     */

    private var isInitialized: Boolean? = null
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

    fun init(context: Context, configuration: ExponeaConfiguration) {
        Logger.i(this, "Init")

        Paper.init(context)

        this.context = context
        this.configuration = configuration

        // Start Network Manager
        this.component = ExponeaComponent(configuration, context)

        // Alarm Manager Starter
        startService()

        // Track our install
        trackInstall()
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
        trackEvent(customerId = customerIds, properties = properties.toHashMap(), route = Route.TRACK_CUSTOMERS)
    }

    /**
     * Track customer event add new events to a specific customer.
     * All events will be stored into coredata until it will be
     * flushed (send it to api).
     */

    fun trackCustomerEvent(customerIds: CustomerIds, properties: PropertiesList, timestamp: Long?, eventType: String?) {
        trackEvent(
                customerId = customerIds,
                properties = properties.toHashMap(),
                timestamp = timestamp,
                eventType = eventType,
                route = Route.TRACK_EVENTS)
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

    private fun trackInstall() {
        val hasInstalled = component.deviceInitiatedRepository.get()

        if (hasInstalled) {
            return
        }

        val device = DeviceProperties(deviceType = component.deviceManager.getDeviceType())

        trackEvent(
                eventType = "installation",
                properties = device.toHashMap(),
                route = Route.TRACK_EVENTS
        )

        component.deviceInitiatedRepository.set(true)
    }

    private fun onFlushPeriodChanged() {
        Logger.d(this, "onFlushPeriodChanged: $flushPeriod")
        startService()
    }

    private fun onFlushModeChanged() {
        Logger.d(this, "onFlushModeChanged: $flushMode")
        when (flushMode) {
            PERIOD -> startService()
        // APP_CLOSE -> //TODO somehow implement this
            MANUAL -> stopService()
        }
    }

    private fun startService() {
        Logger.d(this, "startService")
        component.serviceManager.start()
    }

    private fun stopService() {
        Logger.d(this, "stopService")
        component.serviceManager.stop()
    }
}