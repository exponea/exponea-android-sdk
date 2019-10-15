package com.exponea.sdk.manager

import android.content.Context

interface ServiceManager {
    fun start(context: Context)
    fun stop(context: Context)
}
