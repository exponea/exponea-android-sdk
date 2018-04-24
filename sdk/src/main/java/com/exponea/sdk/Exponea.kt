package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.models.*
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object Exponea {
    private lateinit var context: Context
    private lateinit var configuration: ExponeaConfiguration
    private lateinit var component: ExponeaComponent

    var flushMode: FlushMode = FlushMode.PERIOD
    var flushPeriod: FlushPeriod = FlushPeriod(60, TimeUnit.MINUTES)

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
    }

    /**
     * Send a tracking event to Exponea
     */
    fun trackEvent(
            eventType: String,
            timestamp: Double,
            customerId: CustomerIds,
            properties: HashMap<String, String>
    ) {
        val event = ExportedEventType(
                UUID.randomUUID().toString(),
                eventType,
                timestamp,
                customerId,
                properties
        )

        component.eventManager.addEventToQueue(event)
    }

    /**
     * Manually push all events to Exponea
     */
    fun flush() {
        component.eventManager.flushEvents()
    }
}