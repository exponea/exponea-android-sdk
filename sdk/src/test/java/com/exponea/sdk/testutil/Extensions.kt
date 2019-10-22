package com.exponea.sdk.testutil

import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants

internal fun Exponea.reset() {
    if (!isInitialized) return
    component.campaignRepository.clear()
    component.customerIdsRepository.clear()
    component.deviceInitiatedRepository.set(false)
    component.eventRepository.clear()
    component.firebaseTokenRepository.clear()
    component.pushNotificationRepository.set(false)

    component.sessionManager.stopSessionListener()
    component.serviceManager.stopPeriodicFlush(ApplicationProvider.getApplicationContext())
    component.iapManager.stopObservingPayments()
    component.backgroundTimerManager.stopTimer()
    PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
        .edit().clear().commit()
    flushMode = Constants.Flush.defaultFlushMode
    flushPeriod = Constants.Flush.defaultFlushPeriod
    loggerLevel = Constants.Logger.defaultLoggerLevel
    isInitialized = false
}
