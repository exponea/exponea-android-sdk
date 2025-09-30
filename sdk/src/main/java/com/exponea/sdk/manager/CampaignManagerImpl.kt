package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.exceptions.InvalidConfigurationException
import com.exponea.sdk.models.CampaignData
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.ExponeaServiceImpl
import com.exponea.sdk.network.NetworkHandlerImpl
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.CampaignRepository
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl
import com.exponea.sdk.services.ExponeaProjectFactory
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger

internal class CampaignManagerImpl(
    private val campaignRepository: CampaignRepository,
    private val eventManager: EventManager
) : CampaignManager {

    companion object {
        fun createSdklessInstance(context: Context, configuration: ExponeaConfiguration): CampaignManagerImpl {
            val preferences = ExponeaPreferencesImpl(context)
            val campaignRepository = CampaignRepositoryImpl(ExponeaGson.instance, preferences)
            val eventRepository = TemporaryEventRepositoryImpl(context)
            val uniqueIdentifierRepository = UniqueIdentifierRepositoryImpl(preferences)
            val customerIdsRepository = CustomerIdsRepositoryImpl(
                ExponeaGson.instance, uniqueIdentifierRepository, preferences
            )
            val networkManager = NetworkHandlerImpl(configuration)
            val exponeaService = ExponeaServiceImpl(ExponeaGson.instance, networkManager)
            val connectionManager = ConnectionManagerImpl(context)
            val flushManager = FlushManagerImpl(
                configuration,
                eventRepository,
                exponeaService,
                connectionManager,
                {
                    // no action for identifyCustomer - SDK is not initialized
                }
            )
            val projectFactory = try {
                ExponeaProjectFactory(context, configuration)
            } catch (e: InvalidConfigurationException) {
                if (configuration.advancedAuthEnabled) {
                    Logger.w(this, "Turning off advanced auth for campaign data tracking")
                    configuration.advancedAuthEnabled = false
                }
                ExponeaProjectFactory(context, configuration)
            }
            val eventManager = EventManagerImpl(
                configuration, eventRepository, customerIdsRepository, flushManager, projectFactory,
                onEventCreated = { event, type ->
                    // no action for any event - SDK is not initialized
                },
                deviceId = DeviceIdManager.getDeviceId(context = context)
            )
            return CampaignManagerImpl(campaignRepository, eventManager)
        }
    }

    override fun trackCampaignClick(campaignData: CampaignData): Boolean {
        if (Exponea.isStopped) {
            Logger.e(this, "Campaign event not tracked, SDK is stopping")
            return false
        }
        // store campaign data to be used by session_start event
        campaignRepository.set(campaignData)
        // but stop tracking campaign click if is not valid
        if (!campaignData.isValidForCampaignTrack()) {
            Logger.v(this, "Campaign click not tracked because ${campaignData.completeUrl} is invalid")
            return false
        }
        val properties = HashMap<String, Any>()
        properties["platform"] = "Android"
        properties["timestamp"] = campaignData.createdAt
        properties.putAll(campaignData.getTrackingData())
        eventManager.track(
            eventType = Constants.EventTypes.push,
            properties = properties,
            type = EventType.CAMPAIGN_CLICK
        )
        return true
    }
}
