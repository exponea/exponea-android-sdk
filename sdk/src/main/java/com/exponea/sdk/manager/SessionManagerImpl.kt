package com.exponea.sdk.manager

import com.exponea.sdk.BuildConfig
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.Route
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import java.util.*

class SessionManagerImpl(val prefs: ExponeaPreferences) : SessionManager {

    companion object {
        const val PREF_SESSION_END = "SessionEndTime"
        const val PREF_SESSION_START = "SessionStartTime"
    }

    private fun getSessionLengthInSeconds() : Long {
        val start = prefs.getLong(PREF_SESSION_START, Date().time)
        val end = prefs.getLong(PREF_SESSION_END, Date().time)
        return (end - start) / 1000
    }

    override fun onSessionStart() {
        Logger.d(this, "Session start")
        val lastSessionStart = prefs.getLong(PREF_SESSION_START, 0)
        Logger.d(this, "Last time started: ${Date(lastSessionStart)}")
        val timestamp = Date().time
        Logger.d(this, "Now Started: ${Date(timestamp)}")
        prefs.setLong(PREF_SESSION_START,timestamp)
        trackStart(timestamp)
    }

    override fun onSessionEnd() {
        Logger.d(this, "Session end")
        val lastSessionEnd = prefs.getLong(PREF_SESSION_END, 0)
        Logger.d(this, "Last time end: ${Date(lastSessionEnd)}")
        val timestamp = Date().time
        Logger.d(this, "Now Ended: ${Date(timestamp)}")
        prefs.setLong(PREF_SESSION_END, timestamp)
        trackEnd(timestamp)

    }

    private fun trackStart(timestamp: Long) {

        val properties = DeviceProperties().toHashMap()
        properties["app_version"] = BuildConfig.VERSION_CODE
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = timestamp,
                properties = properties,
                route = Route.TRACK_EVENTS
        )
    }

    private fun trackEnd(timestamp: Long) {
        val properties = DeviceProperties().toHashMap()
        properties["app_version"] = BuildConfig.VERSION_CODE
        properties["duration"] = getSessionLengthInSeconds()
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionEnd,
                timestamp = timestamp,
                properties = properties,
                route = Route.TRACK_EVENTS
        )


    }

}