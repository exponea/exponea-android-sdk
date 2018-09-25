package com.exponea.sdk.manager

import android.app.Application
import android.content.Context
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.*
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.toDate
import java.util.*
import java.util.concurrent.TimeUnit

class SessionManagerImpl(
        context: Context,
        private val prefs: ExponeaPreferences
) : SessionManager() {
    var application = context as Application
    private var isListenerActive = false

    companion object {
        const val PREF_SESSION_END = "SessionEndTimeFloat"
        const val PREF_SESSION_START = "SessionStartTimeFloat"
        const val PREF_SESSION_AUTO_TRACK = "SessionAutomaticTracking"
    }

    /**
     * Calculate session length
     */
    private fun getSessionLengthInSeconds(): Float {
        val start = prefs.getFloat(PREF_SESSION_START, currentTimeSeconds())
        val end = prefs.getFloat(PREF_SESSION_END, currentTimeSeconds())
        Logger.d(
                this, "Session Info: \n " +
                "\t From: ${start.toDate()}\n" +
                "\t To: ${end.toDate()}"
        )

        return (end - start)
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
        // Cancel background timer if set
        Exponea.component.backgroundTimerManager.stopTimer()
        val now = currentTimeSeconds()
        Logger.d(this, "Session start ${now.toDate()}")

        // Check if current session is the first one
        val lastTimeStarted = prefs.getFloat(PREF_SESSION_START, -1f)
        val lastTimeFinished = prefs.getFloat(PREF_SESSION_END, -1f)
        if (lastTimeStarted == -1f || lastTimeFinished == -1f) {
            prefs.setFloat(PREF_SESSION_START, now)
            trackSessionStart(now)
            return
        }

        if (!canBeResumed(now)) {
            Logger.d(this, "New Session Started: ${now.toDate()}")

            // Finish Tracking old session
            trackSessionEnd(now)

            // Start Tracking new session
            prefs.setFloat(PREF_SESSION_START, now)
            trackSessionStart(now)
        }

    }

    /**
     * Method called when app goes to background
     */
    override fun onSessionEnd() {
        val now = currentTimeSeconds()
        Logger.d(this, "Session end ${now.toDate()}")
        prefs.setFloat(PREF_SESSION_END, now)
        // Start background timer to track end of the session
        Exponea.component.backgroundTimerManager.startTimer()
    }

    /**
     * Tracking Session Start
     */
     override fun trackSessionStart(timestamp: Float) {
        Logger.d(this, "Tracking session start at: ${timestamp.toDate()}")

        // Save session start time if session tracking is manual
        if (!isListenerActive) {
            prefs.setFloat(PREF_SESSION_START, timestamp)
        }

        val properties = DeviceProperties().toHashMap()

        properties["app_version"] = BuildConfig.VERSION_CODE
        Exponea.track(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = timestamp,
                properties = properties,
                type = EventType.SESSION_START
        )
    }

    /**
     * Tracking Session End
     */
     override fun trackSessionEnd(timestamp: Float) {
        Logger.d(this, "Tracking session end at: ${timestamp.toDate()}")

        // Save session end time if session tracking is manual
        if (!isListenerActive) {
            prefs.setFloat(PREF_SESSION_END, timestamp)
        }

        val properties = DeviceProperties().toHashMap()
        properties["app_version"] = BuildConfig.VERSION_CODE
        properties["duration"] = getSessionLengthInSeconds()
        Logger.d(this, "Session duration: ${properties["duration"]}")
        // Clear session
        clear()
        Exponea.track(
                eventType = Constants.EventTypes.sessionEnd,
                timestamp = timestamp,
                properties = properties,
                type = EventType.SESSION_END
        )

    }

    /**
     * Determines if current session can be resumed
     * ( i.e session timeout didn't expire )
     */
    private fun canBeResumed(now: Float): Boolean {
        val sessionEnded = prefs.getFloat(PREF_SESSION_END, -1f)
        if (sessionEnded == -1f) return false
        val currentTimeout = (now - sessionEnded)
        return currentTimeout < Exponea.sessionTimeout

    }

    /**
     * Set Session's end and start to default values
     */
    private fun clear() {
        Logger.d(this, "Clearing session Info")
        prefs.setFloat(PREF_SESSION_START, -1f)
        prefs.setFloat(PREF_SESSION_END, -1f)
    }

    override fun reset() {
        clear()
    }
}