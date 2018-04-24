package com.exponea.example.managers

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

@SuppressLint("HardwareIds")
class UserIdManager(private val context: Context) {
    var uniqueUserID: String = ""
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
}