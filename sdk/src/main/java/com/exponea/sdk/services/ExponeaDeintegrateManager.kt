package com.exponea.sdk.services

import android.app.Application
import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.DeviceIdManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.AppInboxCacheImpl
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.HtmlNormalizedCacheImpl
import com.exponea.sdk.repository.InAppContentBlockDisplayStateRepositoryImpl
import com.exponea.sdk.repository.InAppMessageDisplayStateRepositoryImpl
import com.exponea.sdk.repository.InAppMessagesCacheImpl
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.repository.SegmentsCacheImpl
import com.exponea.sdk.repository.UniqueIdentifierRepositoryImpl
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.storage.FileTelemetryStorage
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.logOnException
import java.util.concurrent.ConcurrentLinkedQueue

class ExponeaDeintegrateManager {
    val onIntegrationStoppedCallbacks = ConcurrentLinkedQueue<OnIntegrationStoppedCallback>()
    /**
     * Runs all callbacks that are required to be notified about SDK de-integration.
     * Mainly for stop some processes.
     */
    fun notifyDeintegration() {
        do {
            val pendingCallback = onIntegrationStoppedCallbacks.poll()
            pendingCallback?.let { executeCallbackSafely(it) }
        } while (pendingCallback != null)
        Logger.v(this, "All pending callbacks invoked")
    }
    /**
     * Runs `callback` code on ExponeaSDK de-integration.
     * If SDK is already stopped then callback is not registered.
     */
    fun registerForIntegrationStopped(callback: OnIntegrationStoppedCallback) {
        if (Exponea.isStopped) {
            Logger.e(this, "Callback for SDK de-integration is not registered because SDK is already stopped")
            return
        }
        onIntegrationStoppedCallbacks.add(callback)
    }
    private fun executeCallbackSafely(callback: OnIntegrationStoppedCallback) = runCatching {
        callback.onIntegrationStopped()
    }.logOnException()

    /**
     * Removes `callback` from list of callbacks that are required to be notified about SDK de-integration.
     */
    fun unregisterForIntegrationStopped(callback: OnIntegrationStoppedCallback) {
        onIntegrationStoppedCallbacks.remove(callback)
    }

    /**
     * Removes all SDK local data, caches, etc...
     */
    fun clearLocalCustomerData() {
        val context = ExponeaContextProvider.applicationContext
        if (context == null) {
            Logger.e(this, "Unable to clear all SDK data because application context is null")
        }
        context?.let {
            clearTrackedEvents(it)
            clearAppInbox(it)
            clearSegments(it)
            clearSession(it)
            clearPushToken(it)
            clearCampaignData(it)
            clearInstallEvent(it)
            clearConfiguration(it)
            clearContentBlocks(it)
            clearInAppMessages(it)
            clearResourcesCaches(it)
            clearCookieRepository(it)
            clearCustomerIdsRepository(it)
            clearTelemetry(it)
            DeviceIdManager.clear(it)
        }
    }

    private fun clearTelemetry(context: Context) {
        val application = context.applicationContext as Application
        FileTelemetryStorage(application).clear()
        TelemetryManager.getSharedPreferences(application).edit().clear().apply()
    }

    private fun clearCustomerIdsRepository(context: Context) {
        val prefs = ExponeaPreferencesImpl(context)
        val cookieRepo = UniqueIdentifierRepositoryImpl(prefs)
        CustomerIdsRepositoryImpl(ExponeaGson.instance, cookieRepo, prefs).clear()
    }

    private fun clearCookieRepository(context: Context) {
        val prefs = ExponeaPreferencesImpl(context)
        UniqueIdentifierRepositoryImpl(prefs).clear()
    }

    private fun clearResourcesCaches(context: Context) {
        DrawableCacheImpl(context).clear()
        FontCacheImpl(context).clear()
    }

    private fun clearInAppMessages(context: Context) {
        val prefs = ExponeaPreferencesImpl(context)
        InAppMessagesCacheImpl(context, ExponeaGson.instance).clear()
        InAppMessageDisplayStateRepositoryImpl(prefs, ExponeaGson.instance).clear()
    }

    private fun clearContentBlocks(context: Context) {
        val prefs = ExponeaPreferencesImpl(context)
        InAppContentBlockDisplayStateRepositoryImpl(prefs).clear()
        HtmlNormalizedCacheImpl(context, prefs).clearAll()
    }

    private fun clearConfiguration(context: Context) {
        ExponeaConfigRepository.clear(context)
    }

    private fun clearInstallEvent(context: Context) {
        DeviceInitiatedRepositoryImpl(ExponeaPreferencesImpl(context)).set(false)
    }

    private fun clearCampaignData(context: Context) {
        val prefs = ExponeaPreferencesImpl(context)
        CampaignRepositoryImpl(ExponeaGson.instance, prefs).clear()
    }

    private fun clearPushToken(context: Context) {
        PushTokenRepositoryProvider.get(context).clear()
    }

    private fun clearSession(context: Context) {
        ExponeaPreferencesImpl(context).apply {
            this.remove(SessionManagerImpl.PREF_SESSION_START)
            this.remove(SessionManagerImpl.PREF_SESSION_END)
        }
    }

    private fun clearSegments(context: Context) {
        Exponea.segmentationDataCallbacks.clear()
        SegmentsCacheImpl(context, ExponeaGson.instance).clear()
    }

    private fun clearAppInbox(context: Context) {
        AppInboxCacheImpl(
            context = context,
            gson = ExponeaGson.instance,
            applicationId = ExponeaConfigRepository.get(context)?.applicationId
                ?: Constants.ApplicationId.APP_ID_DEFAULT_VALUE
        ).clear()
    }

    private fun clearTrackedEvents(context: Context) {
        EventRepositoryImpl(context).onIntegrationStopped()
    }
}

interface OnIntegrationStoppedCallback {
    fun onIntegrationStopped()
}
