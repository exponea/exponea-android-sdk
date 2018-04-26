package com.exponea.sdk

import android.content.Context
import com.exponea.sdk.manager.*
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.network.ExponeaServiceImpl
import com.exponea.sdk.network.NetworkHandler
import com.exponea.sdk.network.NetworkHandlerImpl
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.DeviceInitiatedRepository
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.EventRepositoryImpl
import com.google.gson.Gson

class ExponeaComponent(
        exponeaConfiguration: ExponeaConfiguration,
        context: Context
) {
    // Gson Deserializer
    val gson = Gson()
    // Preferences
    val preferences: ExponeaPreferences = ExponeaPreferencesImpl(context)
    // Repositories
    val deviceInitiatedRepository: DeviceInitiatedRepository = DeviceInitiatedRepositoryImpl(preferences)
    val eventRepository: EventRepository = EventRepositoryImpl()
    // Network Handler
    val networkManager: NetworkHandler = NetworkHandlerImpl(exponeaConfiguration)
    // Api Service
    val exponeaService: ExponeaService = ExponeaServiceImpl(gson, networkManager)
    //Managers
    val serviceManager: ServiceManager = ServiceManagerImpl(context)
    val eventManager: EventManager = EventManagerImpl(exponeaConfiguration, eventRepository)
    val flushManager: FlushManager = FlushManagerImpl(exponeaConfiguration, eventRepository, exponeaService)
    val deviceManager: DeviceManager = DeviceManagerImpl(context)
    val fileManager: FileManager = FileManagerImpl(gson)
}