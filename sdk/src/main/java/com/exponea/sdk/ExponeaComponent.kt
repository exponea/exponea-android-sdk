package com.exponea.sdk

import android.content.Context
import com.exponea.sdk.manager.*
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.network.NetworkManager
import com.exponea.sdk.repository.EventRepository
import com.google.gson.Gson

class ExponeaComponent(exponeaConfiguration: ExponeaConfiguration,
                       context: Context) {
    // Repositories
    val eventRepository: EventRepository = EventRepository()
    val gson = Gson()
    //Managers
    val serviceManager: ServiceManager = ServiceManagerImpl(context)
    val networkManager: NetworkManager = NetworkManager(exponeaConfiguration)
    val exponeaApiManager: ExponeaApiManager = ExponeaApiManager(gson, networkManager)
    val eventManager: EventManager = EventManager(exponeaConfiguration,eventRepository, exponeaApiManager)
    val deviceManager: DeviceManager = DeviceManagerImpl(context)
}