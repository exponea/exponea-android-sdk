package com.exponea.sdk.manager

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Context
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import java.util.*

class SessionManagerImpl(val prefs: ExponeaPreferences) : SessionManager {

    companion object {
        const val PREF_SESSION_END = "SessionEndTime"
        const val PREF_SESSION_START = "SessionStartTime"
    }


    override fun onSessionStart() {
        Logger.d(this, "Session start")
        val lastSessionStart = prefs.getLong(PREF_SESSION_START, 0)
        Logger.d(this, "Last time started: ${Date(lastSessionStart)}")
        val timestamp = Date().time
        Logger.d(this, "Now Started: ${Date(timestamp)}")
        prefs.setLong(PREF_SESSION_START,timestamp)
    }

    override fun onSessionEnd() {
        Logger.d(this, "Session end")
        val lastSessionEnd = prefs.getLong(PREF_SESSION_END, 0)
        Logger.d(this, "Last time end: ${Date(lastSessionEnd)}")
        val timestamp = Date().time
        Logger.d(this, "Now Ended: ${Date(timestamp)}")
        prefs.setLong(PREF_SESSION_END, timestamp)

    }

}