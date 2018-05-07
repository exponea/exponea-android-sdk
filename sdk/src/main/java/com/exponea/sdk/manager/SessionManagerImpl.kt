package com.exponea.sdk.manager

import com.exponea.sdk.BuildConfig
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.Route
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import java.util.*

class SessionManagerImpl(private val prefs: ExponeaPreferences) : SessionManager {

    companion object {
        const val PREF_SESSION_END = "SessionEndTime"
        const val PREF_SESSION_START = "SessionStartTime"
    }

    /**
     * Calculate session length
     */
    private fun getSessionLengthInSeconds() : Long {
        val start = prefs.getLong(PREF_SESSION_START, Date().time)
        val end = prefs.getLong(PREF_SESSION_END, Date().time)

        return (end - start) / 1000
    }


    override fun onSessionStart() {
        val now = Date().time
        Logger.d(this, "Session start ${Date(now)}")

        // Check if current session is the first one
        val lastTimeStarted = prefs.getLong(PREF_SESSION_START,  -1L)
        if (lastTimeStarted == -1L) {
            prefs.setLong(PREF_SESSION_START, now)
            return
        }

        if (!canBeResumed(now)) {
            Logger.d(this, "New Session Started: ${Date(now)}")

            // Finish Tracking old session
            trackEnd(now)

            // Start Tracking new session
            prefs.setLong(PREF_SESSION_START,now)
            trackStart(now)
        }

    }


    override fun onSessionEnd() {
        val now = Date().time
        Logger.d(this, "Session end ${Date(now)}")
        prefs.setLong(PREF_SESSION_END, now)
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
        Logger.d(this, "Session duration: ${properties["duration"]}")

        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionEnd,
                timestamp = timestamp,
                properties = properties,
                route = Route.TRACK_EVENTS
        )
    }

    /**
     * Determines if current session can be resumed
     * ( i.e session timeout didn't expire )
     */
    private fun canBeResumed(now: Long) : Boolean {
        val sessionEnded = prefs.getLong(PREF_SESSION_END, -1L)
        val currentTimeout = ( now - sessionEnded ) / 1000
        return currentTimeout < Exponea.sessionTimeout

    }


}