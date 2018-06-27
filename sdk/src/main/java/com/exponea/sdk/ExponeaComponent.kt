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
    internal val gson = Gson()
    // In App Purchase Manager
    internal val iapManager: IapManager = IapManagerImpl(context)
    // Device Manager
    internal val deviceManager: DeviceManager = DeviceManagerImpl(context)
    // Preferences
    internal val preferences: ExponeaPreferences = ExponeaPreferencesImpl(context)
    // Repositories
    internal val deviceInitiatedRepository: DeviceInitiatedRepository = DeviceInitiatedRepositoryImpl(
            preferences
    )
    private val uniqueIdentifierRepository: UniqueIdentifierRepository = UniqueIdentifierRepositoryImpl(
            preferences
    )

    internal val customerIdsRepository: CustomerIdsRepository = CustomerIdsRepositoryImpl(
            gson, uniqueIdentifierRepository, preferences
    )

    internal val pushNotificationRepository: PushNotificationRepository = PushNotificationRepositoryImpl(
            preferences
    )
    internal val eventRepository: EventRepository = EventRepositoryImpl()
    // Network Handler
    internal val networkManager: NetworkHandler = NetworkHandlerImpl(exponeaConfiguration)
    // Api Service
    internal val exponeaService: ExponeaService = ExponeaServiceImpl(gson, networkManager)

    //Managers
    internal val backgroundTimerManager: BackgroundTimerManager = BackgroundTimerManagerImpl(context, exponeaConfiguration)
    internal val serviceManager: ServiceManager = ServiceManagerImpl(context)
    internal val eventManager: EventManager = EventManagerImpl(exponeaConfiguration, eventRepository)
    internal val flushManager: FlushManager = FlushManagerImpl(exponeaConfiguration, eventRepository, exponeaService)
    internal val fcmManager: FcmManager = FcmManagerImpl(context, exponeaConfiguration)
    internal val pushManager: PushManager = PushManagerImpl(uniqueIdentifierRepository)
    internal val fileManager: FileManager = FileManagerImpl()
    internal val personalizationManager: PersonalizationManager = PersonalizationManagerImpl(context)
    internal val fetchManager: FetchManager = FetchManagerImpl(exponeaService, gson)
    internal val sessionManager: SessionManager = SessionManagerImpl(context, preferences)
}