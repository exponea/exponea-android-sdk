package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.FlushPeriod

interface ServiceManager {
    fun startPeriodicFlush(context: Context, flushPeriod: FlushPeriod)
    fun stopPeriodicFlush(context: Context)
}
