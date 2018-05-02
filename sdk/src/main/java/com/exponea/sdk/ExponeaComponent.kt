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
import com.exponea.sdk.repository.*
import com.google.gson.Gson

class ExponeaComponent(
        exponeaConfiguration: ExponeaConfiguration,
        context: Context
) {
    // Gson Deserializer
    val gson = Gson()
    // In App Purchase Manager
    val iapManager: IapManager = IapManagerImpl(context)
    // Device Manager
    val deviceManager: DeviceManager = DeviceManagerImpl(context)
    // Preferences
    val preferences: ExponeaPreferences = ExponeaPreferencesImpl(context)
    // Repositories
    val deviceInitiatedRepository: DeviceInitiatedRepository = DeviceInitiatedRepositoryImpl(preferences)
    val uniqueIdentifierRepository: UniqueIdentifierRepository = UniqueIdentifierRepositoryImpl(preferences)
    val eventRepository: EventRepository = EventRepositoryImpl()
    // Network Handler
    val networkManager: NetworkHandler = NetworkHandlerImpl(exponeaConfiguration)
    // Api Service
    val exponeaService: ExponeaService = ExponeaServiceImpl(gson, networkManager)
    //Managers
    val serviceManager: ServiceManager = ServiceManagerImpl(context)
    val eventManager: EventManager = EventManagerImpl(exponeaConfiguration, eventRepository)
    val flushManager: FlushManager = FlushManagerImpl(exponeaConfiguration, eventRepository, exponeaService)
    val fcmManager: FcmManager = FcmManagerImpl(context, exponeaConfiguration,uniqueIdentifierRepository)
}