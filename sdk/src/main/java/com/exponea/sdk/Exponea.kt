package com.exponea.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.exponea.sdk.manager.DeviceManager
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.util.Logger
import io.paperdb.Paper
import java.util.*

@SuppressLint("StaticFieldLeak")
object Exponea {
    private lateinit var context: Context
    private lateinit var configuration: ExponeaConfiguration
    private lateinit var component: ExponeaComponent
    private lateinit var deviceManager: DeviceManager

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

    fun trackEvent(
            eventType: String,
            timestamp: Double,
            customerId: CustomerIds?,
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

    private fun trackInstall() {
        val hasInstalled = component.preferences.getBoolean("install", false)

        if (hasInstalled) {
            return
        }

        val timestamp = System.currentTimeMillis()
        val device: DeviceProperties = DeviceProperties(deviceType = deviceManager.getDeviceType())

        trackEvent("installation",
                timestamp.toDouble(),
                null,
                device.toHashMap())

        component.preferences.setBoolean("install", true);
    }
}