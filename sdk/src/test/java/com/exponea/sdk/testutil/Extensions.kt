package com.exponea.sdk.testutil

import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.models.Constants

internal fun Exponea.reset() {
    flushMode = Constants.Flush.defaultFlushMode
    flushPeriod = Constants.Flush.defaultFlushPeriod
    if (!isInitialized) return
    componentForTesting.campaignRepository.clear()
    componentForTesting.customerIdsRepository.clear()
    componentForTesting.deviceInitiatedRepository.set(false)
    componentForTesting.eventRepository.clear()
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

internal val Exponea.componentForTesting: ExponeaComponent
    get() {
        val componentField = Exponea::class.java.getDeclaredField("component")
        componentField.isAccessible = true
        return componentField.get(this) as ExponeaComponent
    }
