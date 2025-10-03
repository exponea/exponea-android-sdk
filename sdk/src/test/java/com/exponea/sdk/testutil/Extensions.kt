package com.exponea.sdk.testutil

import android.preference.PreferenceManager
import android.webkit.CookieManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.database.ExponeaDatabase
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.util.ExponeaGson
import io.mockk.clearMocks
import kotlin.test.assertEquals
import kotlin.test.fail

internal fun Exponea.shutdown() {
    val wasInitialized = isInitialized
    isInitialized = false
    flushMode = Constants.Flush.defaultFlushMode
    flushPeriod = Constants.Flush.defaultFlushPeriod
    initGate.clear()
    ExponeaContextProvider.reset()
    if (!wasInitialized) return
    ExponeaDatabase.closeDatabase()
    componentForTesting.sessionManager.stopSessionListener()
    componentForTesting.serviceManager.stopPeriodicFlush(ApplicationProvider.getApplicationContext())
    componentForTesting.backgroundTimerManager.stopTimer()
    loggerLevel = Constants.Logger.defaultLoggerLevel
    (componentForTesting.flushManager as FlushManagerImpl).endsFlushProcess()
    telemetry = null
}

internal fun Exponea.reset() {
    val wasInitialized = isInitialized
    isInitialized = false
    isStopped = false
    resetField(Exponea, "application")
    resetField(Exponea, "configuration")
    safeModeOverride = null
    runDebugModeOverride = null
    segmentationDataCallbacks.clear()
    flushMode = Constants.Flush.defaultFlushMode
    flushPeriod = Constants.Flush.defaultFlushPeriod
    initGate.clear()
    ExponeaContextProvider.reset()
    if (!wasInitialized) return
    componentForTesting.campaignRepository.clear()
    componentForTesting.customerIdsRepository.clear()
    componentForTesting.deviceInitiatedRepository.set(false)
    componentForTesting.eventRepository.onIntegrationStopped()
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

/**
 * Asserts if JSONs are equals ignoring of order of items.
 */
internal fun assertEqualsJsons(
    expected: String?,
    actual: String?,
    message: String? = null
) {
    if (expected == null && actual == null) {
        return
    }
    if (expected == null || actual == null) {
        fail(messagePrefix(message) + "Expected <$expected>, actual <$actual>.")
    }
    // as String comparison is tricky, we compare with HashMap and ArrayList
    // as these are comparable regardless from items order
    val expectedJson = parseJsonNode(ExponeaGson.instance.fromJson(expected, Object::class.java))
    val actualJson = parseJsonNode(ExponeaGson.instance.fromJson(actual, Object::class.java))
    assertEquals(expectedJson, actualJson, message)
}

private fun parseJsonNode(jsonNode: Any?): Any? {
    return when {
        jsonNode is Map<*, *> -> parseJsonObject(jsonNode)
        jsonNode is List<*> -> parseJsonArray(jsonNode)
        else -> jsonNode
    }
}

private fun parseJsonObject(jsonObject: Map<*, *>): HashMap<*, *> {
    val map = HashMap<String?, Any?>()
    jsonObject.forEach {
        map[it.key?.toString()] = parseJsonNode(it.value)
    }
    return map
}

private fun parseJsonArray(jsonArray: List<*>): ArrayList<*> {
    val array = ArrayList<Any?>()
    jsonArray.forEach {
        array.add(parseJsonNode(it))
    }
    return array
}

internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

internal fun Any.resetVerifyMockkCount() {
    clearMocks(
        this,
        answers = false,
        recordedCalls = true,
        childMocks = false,
        verificationMarks = true,
        exclusionRules = false
    )
}
