package com.exponea.sdk.manager

import android.content.Context

interface ServiceManager {
    fun startPeriodicFlush(context: Context)
    fun stopPeriodicFlush(context: Context)
}
