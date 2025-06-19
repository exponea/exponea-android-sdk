package com.exponea.sdk.telemetry

import android.content.Context
import android.os.Looper
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.telemetry.model.ErrorData
import com.exponea.sdk.telemetry.model.ErrorStackTraceElement
import com.exponea.sdk.telemetry.model.ThreadInfo
import kotlin.math.min
import kotlin.reflect.KProperty1

object TelemetryUtility {
    private const val SDK_PACKAGE = "com.exponea"
    private const val MAX_STACK_TRACE_LENGTH = 100

    internal fun getErrorData(e: Throwable): ErrorData {
        return getErrorDataInternal(e, hashSetOf())
    }

    /**
     * There can be loops in cause hierarchy
     * We need to remember which throwables we already visited and break in case of a loop
     */
    private fun getErrorDataInternal(e: Throwable, visitedThrowables: HashSet<Throwable>): ErrorData {
        visitedThrowables.add(e)
        var cause: ErrorData? = null
        if (e.cause != null) {
            val throwableCause = e.cause as Throwable
            if (!visitedThrowables.contains(throwableCause)) {
                cause = getErrorDataInternal(throwableCause, visitedThrowables)
            }
        }
        val stackTrace = parseStackTrace(e.stackTrace)

        return ErrorData(
            type = e.javaClass.name,
            message = e.message ?: "",
            stackTrace = stackTrace,
            cause = cause,
            suppressed = e.suppressed?.map { getErrorDataInternal(it, visitedThrowables) }
        )
    }

    internal fun parseStackTrace(source: Array<StackTraceElement>) =
        source.slice(0 until min(MAX_STACK_TRACE_LENGTH, source.size)).map {
            ErrorStackTraceElement(it.className, it.methodName, it.fileName, it.lineNumber)
        }

    internal fun isSDKRelated(e: Throwable): Boolean {
        var t: Throwable? = e
        var visited = hashSetOf<Throwable>()
        while (t != null && !visited.contains(t)) {
            t.stackTrace.forEach { if (it.className.startsWith(SDK_PACKAGE)) return true }
            visited.add(t)
            t = t.cause
        }
        return false
    }

    internal fun formatConfigurationForTracking(configuration: ExponeaConfiguration): HashMap<String, String> {
        val defaultConfiguration = ExponeaConfiguration()
        val isDefault = { property: KProperty1<ExponeaConfiguration, Any?> ->
            property.get(configuration) == property.get(defaultConfiguration)
        }
        val formatConfigurationProperty = { property: KProperty1<ExponeaConfiguration, Any?> ->
            "${property.get(configuration)}${if (isDefault(property)) " [default]" else ""}"
        }
        return hashMapOf(
            "projectRouteMap"
                to if (configuration.projectRouteMap.isNotEmpty()) "[REDACTED]" else "[]",
            "authorization"
                to if (configuration.authorization.isNullOrEmpty()) "[]" else "[REDACTED]",
            "baseURL"
                to formatConfigurationProperty(ExponeaConfiguration::baseURL),
            "httpLoggingLevel"
                to formatConfigurationProperty(ExponeaConfiguration::httpLoggingLevel),
            "maxTries"
                to formatConfigurationProperty(ExponeaConfiguration::maxTries),
            "sessionTimeout"
                to formatConfigurationProperty(ExponeaConfiguration::sessionTimeout),
            "campaignTTL"
                to formatConfigurationProperty(ExponeaConfiguration::campaignTTL),
            "automaticSessionTracking"
                to formatConfigurationProperty(ExponeaConfiguration::automaticSessionTracking),
            "automaticPushNotification"
                to formatConfigurationProperty(ExponeaConfiguration::automaticPushNotification),
            "pushIcon"
                to formatConfigurationProperty(ExponeaConfiguration::pushIcon),
            "pushAccentColor"
                to formatConfigurationProperty(ExponeaConfiguration::pushAccentColor),
            "pushChannelName"
                to formatConfigurationProperty(ExponeaConfiguration::pushChannelName),
            "pushChannelDescription"
                to formatConfigurationProperty(ExponeaConfiguration::pushChannelDescription),
            "pushChannelId"
                to formatConfigurationProperty(ExponeaConfiguration::pushChannelId),
            "pushNotificationImportance"
                to formatConfigurationProperty(ExponeaConfiguration::pushNotificationImportance),
            "defaultProperties"
                to if (configuration.defaultProperties.isNotEmpty()) "[REDACTED]" else "[]",
            "tokenTrackFrequency"
                to formatConfigurationProperty(ExponeaConfiguration::tokenTrackFrequency),
            "allowDefaultCustomerProperties"
                to formatConfigurationProperty(ExponeaConfiguration::allowDefaultCustomerProperties),
            "advancedAuthEnabled"
                to formatConfigurationProperty(ExponeaConfiguration::advancedAuthEnabled),
            "inAppContentBlockPlaceholdersAutoLoad"
                to formatConfigurationProperty(ExponeaConfiguration::inAppContentBlockPlaceholdersAutoLoad),
            "appInboxDetailImageInset"
                to formatConfigurationProperty(ExponeaConfiguration::appInboxDetailImageInset),
            "allowWebViewCookies"
                to formatConfigurationProperty(ExponeaConfiguration::allowWebViewCookies),
            "manualSessionAutoClose"
                to formatConfigurationProperty(ExponeaConfiguration::manualSessionAutoClose)
        )
    }

    internal data class AppInfo(
        val packageName: String,
        val versionName: String,
        val versionCode: String,
        val appName: String
    )

    internal fun getAppInfo(context: Context): AppInfo {
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val applicationInfo = context.applicationInfo
            @Suppress("DEPRECATION")
            return AppInfo(
                packageInfo.packageName,
                packageInfo.versionName ?: "unknown version",
                packageInfo.versionCode.toString(),
                applicationInfo.loadLabel(packageManager).toString()
            )
        } catch (e: Exception) {
            return AppInfo("unknown package", "unknown version", "unknown version code", "unknown app name")
        }
    }

    internal fun getThreadInfo(source: Thread, stackTraceOverride: Array<StackTraceElement>?): ThreadInfo {
        return ThreadInfo(
            id = source.id,
            name = source.name,
            state = source.state.name,
            isDaemon = source.isDaemon,
            isCurrent = source.id == Thread.currentThread().id,
            isMain = source.id == Looper.getMainLooper().thread.id,
            stackTrace = parseStackTrace(stackTraceOverride ?: source.stackTrace)
        )
    }
}
