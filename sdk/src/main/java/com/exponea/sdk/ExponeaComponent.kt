package com.exponea.sdk

import android.content.Context
import com.exponea.sdk.manager.BackgroundTimerManager
import com.exponea.sdk.manager.BackgroundTimerManagerImpl
import com.exponea.sdk.manager.ConnectionManager
import com.exponea.sdk.manager.ConnectionManagerImpl
import com.exponea.sdk.manager.EventManager
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.manager.EventManagerInAppMessageTrackingDelegate
import com.exponea.sdk.manager.FcmManager
import com.exponea.sdk.manager.FcmManagerImpl
import com.exponea.sdk.manager.FetchManager
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.FlushManager
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.manager.InAppMessageManager
import com.exponea.sdk.manager.InAppMessageManagerImpl
import com.exponea.sdk.manager.InAppMessageTrackingDelegate
import com.exponea.sdk.manager.PushNotificationSelfCheckManager
import com.exponea.sdk.manager.PushNotificationSelfCheckManagerImpl
import com.exponea.sdk.manager.ServiceManager
import com.exponea.sdk.manager.ServiceManagerImpl
import com.exponea.sdk.manager.SessionManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.manager.TrackingConsentManager
import com.exponea.sdk.manager.TrackingConsentManagerImpl
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExponeaProject
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
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.repository.InAppMessageDisplayStateRepositoryImpl
import com.exponea.sdk.repository.InAppMessagesCache
import com.exponea.sdk.repository.InAppMessagesCacheImpl
import com.exponea.sdk.repository.PushNotificationRepository
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepository
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.repository.UniqueIdentifierRepository
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.TokenType
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.view.InAppMessagePresenter

internal class ExponeaComponent(
    var exponeaConfiguration: ExponeaConfiguration,
    context: Context
) {
    private val application = context.applicationContext

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

    internal val eventRepository: EventRepository = EventRepositoryImpl(context, preferences)

    internal val pushTokenRepository: PushTokenRepository = PushTokenRepositoryProvider.get(context)

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

    internal val inAppMessagesBitmapCache = InAppMessageBitmapCacheImpl(context)
    internal val inAppMessagePresenter = InAppMessagePresenter(context, inAppMessagesBitmapCache)

    internal val flushManager: FlushManager = FlushManagerImpl(
        exponeaConfiguration,
        eventRepository,
        exponeaService,
        connectionManager,
        // once customer is identified(and potentially merged with another), refresh the in-app messages
        { inAppMessageManager.preload() }
    )

    internal val eventManager: EventManager = EventManagerImpl(
        exponeaConfiguration, eventRepository, customerIdsRepository, flushManager,
        onEventCreated = { event, type ->
            inAppMessageManager.onEventCreated(event, type)
        }
    )

    internal val fcmManager: FcmManager = FcmManagerImpl(
        context, exponeaConfiguration, eventManager, pushTokenRepository, pushNotificationRepository
    )

    internal val sessionManager: SessionManager = SessionManagerImpl(
        context, preferences, campaignRepository, eventManager, backgroundTimerManager
    )

    internal val pushNotificationSelfCheckManager: PushNotificationSelfCheckManager =
        PushNotificationSelfCheckManagerImpl(
            context,
            exponeaConfiguration,
            customerIdsRepository,
            pushTokenRepository,
            flushManager,
            exponeaService
        )

    internal val inAppMessageTrackingDelegate: InAppMessageTrackingDelegate = EventManagerInAppMessageTrackingDelegate(
        context, eventManager
    )

    internal val trackingConsentManager: TrackingConsentManager = TrackingConsentManagerImpl(
        eventManager, campaignRepository, inAppMessageTrackingDelegate
    )

    internal val inAppMessageManager: InAppMessageManager = InAppMessageManagerImpl(
        exponeaConfiguration,
        customerIdsRepository,
        inAppMessagesCache,
        fetchManager,
        inAppMessageDisplayStateRepository,
        inAppMessagesBitmapCache,
        inAppMessagePresenter,
        trackingConsentManager
    )

    fun anonymize(
        exponeaProject: ExponeaProject,
        projectRouteMap: Map<EventType, List<ExponeaProject>> = hashMapOf()
    ) {
        val token = pushTokenRepository.get()
        val tokenType = pushTokenRepository.getLastTokenType()

        // Do not use TokenFrequency from the configuration, clear tokens immediately during anonymize
        fcmManager.trackToken(" ", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, TokenType.FCM)
        fcmManager.trackToken(" ", ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, TokenType.HMS)
        deviceInitiatedRepository.set(false)
        campaignRepository.clear()
        inAppMessagesCache.clear()
        inAppMessageDisplayStateRepository.clear()
        uniqueIdentifierRepository.clear()
        customerIdsRepository.clear()
        sessionManager.reset()

        exponeaConfiguration.baseURL = exponeaProject.baseUrl
        exponeaConfiguration.projectToken = exponeaProject.projectToken
        exponeaConfiguration.authorization = exponeaProject.authorization
        exponeaConfiguration.projectRouteMap = projectRouteMap
        ExponeaConfigRepository.set(application, exponeaConfiguration)

        Exponea.trackInstallEvent()
        if (exponeaConfiguration.automaticSessionTracking) {
            sessionManager.trackSessionStart(currentTimeSeconds())
        }
        // Do not use TokenFrequency from the configuration, setup token from new customer immediately during anonymize
        fcmManager.trackToken(token, ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH, tokenType)
        inAppMessageManager.preload()
    }
}
