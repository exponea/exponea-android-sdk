package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.models.*
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.models.FlushMode.PERIOD
import com.exponea.sdk.services.ExponeaJobService
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object Exponea {
    private lateinit var context: Context
    private lateinit var configuration: ExponeaConfiguration
    private lateinit var component: ExponeaComponent

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
    }


    /**
     * Send a tracking event to Exponea
     */

    fun trackEvent(
            eventType: String?,
            timestamp: Double?,
            customerId: CustomerIds?,
            properties: HashMap<String, Any>
    ) {

        val time: Double = if (timestamp == null) {
            System.currentTimeMillis().toDouble()
        } else {
            timestamp
        }

        val event = ExportedEventType(
                UUID.randomUUID().toString(),
                eventType,
                time,
                customerId,
                properties
        )

        component.eventManager.addEventToQueue(event)
    }

    fun trackCustomer(customerIds: CustomerIds, properties: PropertiesList) {
        trackEvent(null, null, customerIds, properties.toHashMap())
    }

    /**
     * Manually push all events to Exponea
     */
    fun flush() {
        if (ExponeaJobService.isRunning) {
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
        val timestamp = System.currentTimeMillis().toDouble()

        trackEvent(
                "installation",
                timestamp,
                null,
                device.toHashMap()
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