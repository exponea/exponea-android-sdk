package com.exponea.sdk

import android.content.Context
import com.exponea.sdk.manager.DeviceManager
import com.exponea.sdk.manager.EventManager
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.ExponeaApiManager
import com.exponea.sdk.network.NetworkManager
import com.exponea.sdk.repository.EventRepository
import com.google.gson.Gson

class ExponeaComponent(exponeaConfiguration: ExponeaConfiguration,
                       context: Context) {
    // Repositories
    var eventRepository: EventRepository = EventRepository()
    var gson = Gson()
    //Managers
    var networkManager: NetworkManager = NetworkManager(exponeaConfiguration)
    var exponeaApiManager: ExponeaApiManager = ExponeaApiManager(gson, networkManager)
    var eventManager: EventManager = EventManager(eventRepository, exponeaApiManager)
    var deviceManager: DeviceManager = DeviceManager(context)
}