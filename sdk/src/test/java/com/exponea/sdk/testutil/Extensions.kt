package com.exponea.sdk.testutil

import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.repository.EventRepositoryImpl

internal fun Exponea.shutdown() {
    isInitialized = false
    flushMode = Constants.Flush.defaultFlushMode
    flushPeriod = Constants.Flush.defaultFlushPeriod
    (componentForTesting.eventRepository as EventRepositoryImpl).close()
    componentForTesting.sessionManager.stopSessionListener()
    componentForTesting.serviceManager.stopPeriodicFlush(ApplicationProvider.getApplicationContext())
    componentForTesting.backgroundTimerManager.stopTimer()
    loggerLevel = Constants.Logger.defaultLoggerLevel
    (componentForTesting.flushManager as FlushManagerImpl).isRunning = false
    initGate.clear()
    telemetry = null
}

internal fun Exponea.reset() {
    flushMode = Constants.Flush.defaultFlushMode
    flushPeriod = Constants.Flush.defaultFlushPeriod
    initGate.clear()
    if (!isInitialized) return
    componentForTesting.campaignRepository.clear()
    componentForTesting.customerIdsRepository.clear()
    componentForTesting.deviceInitiatedRepository.set(false)
    componentForTesting.eventRepository.clear()
    (componentForTesting.eventRepository as EventRepositoryImpl).close()
    componentForTesting.pushTokenRepository.clear()
    componentForTesting.pushNotificationRepository.set(false)

    componentForTesting.sessionManager.stopSessionListener()
    componentForTesting.serviceManager.stopPeriodicFlush(ApplicationProvider.getApplicationContext())
    componentForTesting.backgroundTimerManager.stopTimer()
    PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
        .edit().clear().commit()
    loggerLevel = Constants.Logger.defaultLoggerLevel
    isInitialized = false
    telemetry = null
}

internal fun EventRepositoryImpl.close() {
    database.openHelper.close()
}

internal val Exponea.componentForTesting: ExponeaComponent
    get() {
        val componentField = Exponea::class.java.getDeclaredField("component")
        componentField.isAccessible = true
        return componentField.get(this) as ExponeaComponent
    }
