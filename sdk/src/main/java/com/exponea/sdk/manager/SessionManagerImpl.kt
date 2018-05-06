package com.exponea.sdk.manager

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Context
import android.os.Bundle
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger

class SessionManagerImpl(val prefs: ExponeaPreferences) : SessionManager {

    companion object {
        const val TAG = "SessionManager"
    }


    override fun onSessionStart() {
        Logger.d(TAG, "Session End")

    }

    override fun onSessionEnd() {
        Logger.d(TAG, "Session start")

    }

}