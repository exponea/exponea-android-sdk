package com.exponea.sdk

import android.content.Context
import com.exponea.sdk.manager.BackgroundTimerManager
import com.exponea.sdk.manager.BackgroundTimerManagerImpl
import com.exponea.sdk.manager.ConnectionManager
import com.exponea.sdk.manager.ConnectionManagerImpl
import com.exponea.sdk.manager.DisabledInAppMessageManagerImpl
import com.exponea.sdk.manager.EventManager
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.manager.FcmManager
import com.exponea.sdk.manager.FcmManagerImpl
import com.exponea.sdk.manager.FetchManager
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.FileManager
import com.exponea.sdk.manager.FileManagerImpl
import com.exponea.sdk.manager.FlushManager
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.manager.InAppMessageManager
import com.exponea.sdk.manager.InAppMessageManagerImpl
import com.exponea.sdk.manager.PersonalizationManager
import com.exponea.sdk.manager.PersonalizationManagerImpl
import com.exponea.sdk.manager.ServiceManager
import com.exponea.sdk.manager.ServiceManagerImpl
import com.exponea.sdk.manager.SessionManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.ExponeaService
import com.exponea.sdk.network.ExponeaServiceImpl
import com.exponea.sdk.network.NetworkHandler
import com.exponea.sdk.network.NetworkHandlerImpl
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.CampaignRepository
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.CustomerIdsRepository
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.repository.DeviceInitiatedRepository
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.repository.FirebaseTokenRepository
import com.exponea.sdk.repository.FirebaseTokenRepositoryImpl
import com.exponea.sdk.repository.InAppMessageDisplayStateRepositoryImpl
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.repository.InAppMessagesCacheImpl
import com.exponea.sdk.repository.PushNotificationRepository
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.UniqueIdentifierRepository
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.currentTimeSeconds
internal class ExponeaComponent(
    exponeaConfiguration: ExponeaConfiguration,
    context: Context
) {

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
            ExponeaGson.instance, uniqueIdentifierRepository, preferences
    )

    internal val pushNotificationRepository: PushNotificationRepository = PushNotificationRepositoryImpl(
            preferences
    )

    internal val eventRepository: EventRepository = EventRepositoryImpl(context)

    internal val firebaseTokenRepository: FirebaseTokenRepository = FirebaseTokenRepositoryImpl(preferences)

    internal val campaignRepository: CampaignRepository = CampaignRepositoryImpl(ExponeaGson.instance, preferences)

    internal val inAppMessagesCache: InAppMessagesCache = InAppMessagesCacheImpl(context, ExponeaGson.instance)

    internal val inAppMessageDisplayStateRepository =
        InAppMessageDisplayStateRepositoryImpl(preferences, ExponeaGson.instance)

    // Network Handler
    internal val networkManager: NetworkHandler = NetworkHandlerImpl(exponeaConfiguration)

    // Api Service
    internal val exponeaService: ExponeaService = ExponeaServiceImpl(ExponeaGson.instance, networkManager)

    // Managers
    internal val fetchManager: FetchManager = FetchManagerImpl(exponeaService, ExponeaGson.instance)

    internal val backgroundTimerManager: BackgroundTimerManager = BackgroundTimerManagerImpl(
        context, exponeaConfiguration
    )

    internal val serviceManager: ServiceManager = ServiceManagerImpl()

    internal val connectionManager: ConnectionManager = ConnectionManagerImpl(context)

    internal val inAppMessageManager: InAppMessageManager
    init {
        inAppMessageManager = if (exponeaConfiguration.inAppMessagesEnabledBETA) {
            InAppMessageManagerImpl(
                context,
                exponeaConfiguration,
                customerIdsRepository,
                inAppMessagesCache,
                fetchManager,
                inAppMessageDisplayStateRepository
            )
        } else {
            DisabledInAppMessageManagerImpl()
        }
    }

    internal val flushManager: FlushManager =
        FlushManagerImpl(exponeaConfiguration, eventRepository, exponeaService, connectionManager)

    internal val eventManager: EventManager = EventManagerImpl(
        context, exponeaConfiguration, eventRepository, customerIdsRepository, flushManager, inAppMessageManager
    )

    internal val fcmManager: FcmManager = FcmManagerImpl(
        context, exponeaConfiguration, eventManager, firebaseTokenRepository, pushNotificationRepository
    )

    internal val fileManager: FileManager = FileManagerImpl()

    internal val personalizationManager: PersonalizationManager = PersonalizationManagerImpl(
        context, fetchManager, fileManager
    )

    internal val sessionManager: SessionManager = SessionManagerImpl(
        context, preferences, campaignRepository, eventManager, backgroundTimerManager
    )

    fun anonymize() {
        val firebaseToken = firebaseTokenRepository.get()
        fcmManager.trackFcmToken(" ", Exponea.tokenTrackFrequency)
        campaignRepository.clear()
        inAppMessagesCache.clear()
        inAppMessageDisplayStateRepository.clear()
        eventRepository.clear()
        uniqueIdentifierRepository.clear()
        customerIdsRepository.clear()
        sessionManager.reset()
        sessionManager.trackSessionStart(currentTimeSeconds())
        fcmManager.trackFcmToken(firebaseToken, Exponea.tokenTrackFrequency)
    }
}
