package com.exponea.example

import android.app.Application
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Exponea.init(this)
        Exponea.loggerLevel = Logger.Level.DEBUG
    }
}