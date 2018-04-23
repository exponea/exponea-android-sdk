package com.exponea.sdk.network

import okhttp3.OkHttpClient

class NetworkManager {
    lateinit var networkClient: OkHttpClient

    init {
        setupNetworkClient()
    }

    private fun setupNetworkClient() {
        networkClient = OkHttpClient.Builder()
                .build()
    }
}