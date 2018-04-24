package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.manager.DeviceManager
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.util.Logger
import java.util.*

@SuppressLint("StaticFieldLeak")
object Exponea {
    private lateinit var context: Context
    private lateinit var configuration: ExponeaConfiguration
    private lateinit var component: ExponeaComponent

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

        this.context = context
        this.configuration = configuration

        // Start Network Manager
        this.component = ExponeaComponent(configuration, context)
    }

    fun trackEvent(
            eventType: String,
            timestamp: Double,
            customerId: HashMap<String, String>,
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
}