package com.exponea.sdk.testutil

import android.preference.PreferenceManager
import android.webkit.CookieManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.repository.EventRepositoryImpl
import kotlin.test.fail

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
    val wasInitialized = isInitialized
    isInitialized = false
    resetField(Exponea, "application")
    resetField(Exponea, "configuration")
    safeModeOverride = null
    runDebugModeOverride = null
    segmentationDataCallbacks.clear()
    flushMode = Constants.Flush.defaultFlushMode
    flushPeriod = Constants.Flush.defaultFlushPeriod
    initGate.clear()
    if (!wasInitialized) return
    componentForTesting.campaignRepository.clear()
    componentForTesting.customerIdsRepository.clear()
    componentForTesting.deviceInitiatedRepository.set(false)
    componentForTesting.eventRepository.clear()
    (componentForTesting.eventRepository as EventRepositoryImpl).close()
    componentForTesting.pushTokenRepository.clear()
    componentForTesting.inAppContentBlockManager.clearAll()
    componentForTesting.segmentsManager.clearAll()

    componentForTesting.sessionManager.stopSessionListener()
    componentForTesting.serviceManager.stopPeriodicFlush(ApplicationProvider.getApplicationContext())
    componentForTesting.backgroundTimerManager.stopTimer()
    PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
        .edit().clear().commit()
    loggerLevel = Constants.Logger.defaultLoggerLevel
    telemetry = null
    CookieManager.getInstance().setAcceptCookie(true)
}

fun resetField(target: Any, fieldName: String) {
    val field = target.javaClass.getDeclaredField(fieldName)
    with(field) {
        isAccessible = true
        set(target, null)
    }
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

/**
 * Asserts if iterables are equals ignoring of order of items.
 */
internal fun assertEqualsIgnoreOrder(
    expected: Collection<Any>?,
    actual: Collection<Any>?,
    message: String? = null
) {
    if (expected == null && actual == null) {
        return
    }
    if (expected == null || actual == null) {
        fail(messagePrefix(message) + "Expected <$expected>, actual <$actual>.")
    }
    if (expected.count() != actual.count()) {
        fail(messagePrefix(message) + "Expected <$expected>, actual <$actual>.")
    }
    val expectedCopy = expected.toTypedArray().sortBy { it.hashCode() }
    val actualCopy = actual.toTypedArray().sortBy { it.hashCode() }
    if (expectedCopy != actualCopy) {
        fail(messagePrefix(message) + "Expected <$expected>, actual <$actual>.")
    }
}

internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "
