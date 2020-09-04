package com.exponea.sdk.manager

import android.content.Context
import android.net.ConnectivityManager

internal class ConnectionManagerImpl(context: Context) : ConnectionManager {
    private val application = context.applicationContext

    override fun isConnectedToInternet(): Boolean {
        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}
