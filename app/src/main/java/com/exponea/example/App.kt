package com.exponea.example

import android.app.Application
import com.exponea.example.managers.UserIdManager
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.FlushPeriod
import com.exponea.sdk.util.Logger
import java.util.concurrent.TimeUnit

class App : Application() {
    companion object {
        lateinit var instance: App
    }

    lateinit var userIdManager: UserIdManager

    override fun onCreate() {
        super.onCreate()

        // Assign our instance to this
        instance = this

        // Create our UserIDManager for getting our unique ID
        userIdManager = UserIdManager(this)

        // Start our exponea configuration
        val configuration = ExponeaConfiguration()

        configuration.authorization = BuildConfig.AuthorizationToken
        configuration.projectToken = BuildConfig.DefaultProjectToken

        // Start our SDK
        Exponea.init(this, configuration, null)
        // Set our debug level to debug
        Exponea.loggerLevel = Logger.Level.DEBUG
        // Set up our flushing
        Exponea.flushMode = FlushMode.PERIOD
        Exponea.flushPeriod = FlushPeriod(1, TimeUnit.MINUTES)
    }


}