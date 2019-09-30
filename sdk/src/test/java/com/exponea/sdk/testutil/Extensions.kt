package com.exponea.sdk.testutil

import com.exponea.sdk.Exponea

fun Exponea.reset() {
    if (!isInitialized) return
    component.eventRepository.clear()
    component.campaignRepository.clear()
    component.sessionManager.stopSessionListener()
    component.serviceManager.stop()
    component.iapManager.stopObservingPayments()
    component.backgroundTimerManager.stopTimer()
    isInitialized = false
}