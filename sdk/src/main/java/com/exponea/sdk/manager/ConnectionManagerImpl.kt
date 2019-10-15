package com.exponea.sdk.manager

import android.content.Context
import android.net.ConnectivityManager

class ConnectionManagerImpl(private val context: Context) : ConnectionManager {
    override fun isConnectedToInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}

