package com.exponea.sdk.manager

import android.app.Application
import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.EventType
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.repository.CampaignRepository
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.exponea.sdk.util.toDate

internal class SessionManagerImpl(
    context: Context,
    private val prefs: ExponeaPreferences,
    private val campaignRepository: CampaignRepository,
    private val eventManager: EventManager,
    private val backgroundTimerManager: BackgroundTimerManager
) : SessionManager() {
    var application = context as Application
    private var isListenerActive = false

    companion object {
        const val PREF_SESSION_END = "SessionEndTimeDouble"
        const val PREF_SESSION_START = "SessionStartTimeDouble"
    }

    /**
     * Calculate session length
     */
    private fun getSessionLengthInSeconds(): Double {
        val start = prefs.getDouble(PREF_SESSION_START, currentTimeSeconds())
        val end = prefs.getDouble(PREF_SESSION_END, currentTimeSeconds())
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
        }
    }

    /**
     * Stops session listener
     */
    override fun stopSessionListener() {
        if (isListenerActive) {
            application.unregisterActivityLifecycleCallbacks(this)
            isListenerActive = false
        }
    }

    /**
     *  Method called when app is in foreground
     */
    override fun onSessionStart() {
        // Cancel background timer if set
        backgroundTimerManager.stopTimer()
        val now = currentTimeSeconds()
        Logger.d(this, "Session start ${now.toDate()}")

        // Check if current session is the first one
        val lastTimeStarted = prefs.getDouble(PREF_SESSION_START, -1.0)
        val lastTimeFinished = prefs.getDouble(PREF_SESSION_END, -1.0)
        if (lastTimeStarted == -1.0 || lastTimeFinished == -1.0) {
            prefs.setDouble(PREF_SESSION_START, now)
            trackSessionStart(now)
        } else if (!canBeResumed(now)) {
            Logger.d(this, "New Session Started: ${now.toDate()}")

            // Finish Tracking old session
            trackSessionEnd(now)

            // Start Tracking new session
            prefs.setDouble(PREF_SESSION_START, now)
            trackSessionStart(now)
        }
        campaignRepository.clear()
    }

    /**
     * Method called when app goes to background
     */
    override fun onSessionEnd() {
        val now = currentTimeSeconds()
        Logger.d(this, "Session end ${now.toDate()}")
        prefs.setDouble(PREF_SESSION_END, now)
        // Start background timer to track end of the session
        backgroundTimerManager.startTimer()
    }

    /**
     * Tracking Session Start
     */
    override fun trackSessionStart(timestamp: Double) {
        Logger.d(this, "Tracking session start at: ${timestamp.toDate()}")

        // Save session start time if session tracking is manual
        if (!isListenerActive) {
            prefs.setDouble(PREF_SESSION_START, timestamp)
        }
        val properties = DeviceProperties(application).toHashMap()
        campaignRepository.get()?.let { event ->
            properties["location"] = event.completeUrl
            properties["utm_source"] = event.source.orEmpty()
            properties["utm_campaign"] = event.campaign.orEmpty()
            properties["utm_content"] = event.content.orEmpty()
            properties["utm_term"] = event.term.orEmpty()
        }
        eventManager.track(
            eventType = Constants.EventTypes.sessionStart,
            timestamp = timestamp,
            properties = properties,
            type = EventType.SESSION_START
        )
    }

    /**
     * Tracking Session End
     */
    override fun trackSessionEnd(timestamp: Double) {
        Logger.d(this, "Tracking session end at: ${timestamp.toDate()}")

        // Save session end time if session tracking is manual
        if (!isListenerActive) {
            prefs.setDouble(PREF_SESSION_END, timestamp)
        }

        val properties = DeviceProperties(application).toHashMap()
        properties["duration"] = getSessionLengthInSeconds()
        Logger.d(this, "Session duration: ${properties["duration"]}")
        // Clear session
        clear()
        eventManager.track(
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
    private fun canBeResumed(now: Double): Boolean {
        val sessionEnded = prefs.getDouble(PREF_SESSION_END, -1.0)
        if (sessionEnded == -1.0) return false
        val currentTimeout = (now - sessionEnded)
        return currentTimeout < Exponea.sessionTimeout
    }

    /**
     * Set Session's end and start to default values
     */
    private fun clear() {
        Logger.d(this, "Clearing session Info")
        prefs.setDouble(PREF_SESSION_START, -1.0)
        prefs.setDouble(PREF_SESSION_END, -1.0)
    }

    override fun reset() {
        clear()
    }
}
