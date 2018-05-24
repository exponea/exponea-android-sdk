package com.exponea.sdk.manager

import android.app.Application
import android.content.Context
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.Route
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import java.util.*

class SessionManagerImpl(
        context: Context,
        private val prefs: ExponeaPreferences
) : SessionManager() {
    var application = context as Application
    private var isListenerActive = false

    companion object {
        const val PREF_SESSION_END = "SessionEndTime"
        const val PREF_SESSION_START = "SessionStartTime"
        const val PREF_SESSION_AUTO_TRACK = "SessionAutomaticTracking"
    }

    /**
     * Calculate session length
     */
    private fun getSessionLengthInSeconds(): Long {
        val start = prefs.getLong(PREF_SESSION_START, Date().time)
        val end = prefs.getLong(PREF_SESSION_END, Date().time)
        Logger.d(
                this, "Session Info: \n " +
                "\t From: ${Date(start)}\n" +
                "\t To: ${Date(end)}"
        )

        return (end - start) / 1000
    }

    /**
     * Starts session listener by enabling activityLifecycleCallbacks
     */
    override fun startSessionListener() {
        if (!isListenerActive) {
            application.registerActivityLifecycleCallbacks(this)
            isListenerActive = true
            prefs.setBoolean(PREF_SESSION_AUTO_TRACK, true)
        }
    }

    /**
     * Stops session listener
     */
    override fun stopSessionListener() {
        if (isListenerActive) {
            application.unregisterActivityLifecycleCallbacks(this)
            isListenerActive = false
            prefs.setBoolean(PREF_SESSION_AUTO_TRACK, false)
        }
    }

    /**
     *  Method called when app is in foreground
     */
    override fun onSessionStart() {
        val now = Date().time
        Logger.d(this, "Session start ${Date(now)}")

        // Check if current session is the first one
        val lastTimeStarted = prefs.getLong(PREF_SESSION_START, -1L)
        if (lastTimeStarted == -1L) {
            prefs.setLong(PREF_SESSION_START, now)
            return
        }

        if (!canBeResumed(now)) {
            Logger.d(this, "New Session Started: ${Date(now)}")

            // Finish Tracking old session
            trackSessionEnd(now)

            // Start Tracking new session
            prefs.setLong(PREF_SESSION_START, now)
            trackSessionStart(now)
        }

    }

    /**
     * Method called when app goes to background
     */
    override fun onSessionEnd() {
        val now = Date().time
        Logger.d(this, "Session end ${Date(now)}")
        prefs.setLong(PREF_SESSION_END, now)
    }

    /**
     * Tracking Session Start
     */
     override fun trackSessionStart(timestamp: Long) {
        Logger.d(this, "Tracking session start at: ${Date(timestamp)}")

        if (!isListenerActive) {
            prefs.setLong(PREF_SESSION_START, timestamp)
        }

        val properties = DeviceProperties().toHashMap()
        properties["app_version"] = BuildConfig.VERSION_CODE
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = timestamp,
                properties = properties,
                route = Route.TRACK_EVENTS
        )
    }

    /**
     * Tracking Session End
     */
     override fun trackSessionEnd(timestamp: Long) {
        Logger.d(this, "Tracking session end at: ${Date(timestamp)}")
        if (!isListenerActive) {
            prefs.setLong(PREF_SESSION_END, timestamp)
        }

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
    private fun canBeResumed(now: Long): Boolean {
        val sessionEnded = prefs.getLong(PREF_SESSION_END, -1L)
        val currentTimeout = (now - sessionEnded) / 1000
        return currentTimeout < Exponea.sessionTimeout

    }


}