package com.exponea.example

import android.app.Application
import com.exponea.example.logging.MemoryLogger
import com.exponea.example.managers.RegisteredIdManager

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    lateinit var registeredIdManager: RegisteredIdManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        registeredIdManager = RegisteredIdManager(this)
        MemoryLogger.INSTANCE.start()
    }
}
