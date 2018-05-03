package com.exponea.example

import android.app.Application
import com.exponea.example.managers.UserIdManager

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

    }


}