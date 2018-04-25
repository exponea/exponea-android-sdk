package com.exponea.sdk

import android.content.Context
import com.exponea.sdk.manager.*
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.network.NetworkManager
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.DeviceInitiatedRepository
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.repository.EventRepository
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
    val eventRepository: EventRepository = EventRepository()
    //Managers
    val serviceManager: ServiceManager = ServiceManagerImpl(context)
    val networkManager: NetworkManager = NetworkManager(exponeaConfiguration)
    val exponeaApiManager: ExponeaApiManager = ExponeaApiManager(gson, networkManager)
    val eventManager: EventManager = EventManager(
            exponeaConfiguration,
            eventRepository,
            exponeaApiManager
    )
    val deviceManager: DeviceManager = DeviceManagerImpl(context)
}