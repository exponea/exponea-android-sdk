package com.exponea.example

import android.app.Application
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.Logger

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val configuration = ExponeaConfiguration()

        configuration.authorization = BuildConfig.AuthorizationToken
        configuration.projectToken = BuildConfig.DefaultProjectToken

        Exponea.init(this, configuration)
        Exponea.loggerLevel = Logger.Level.DEBUG
    }
}