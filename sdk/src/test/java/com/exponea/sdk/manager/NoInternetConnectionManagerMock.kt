package com.exponea.sdk.manager

internal object NoInternetConnectionManagerMock : ConnectionManager {

    override fun isConnectedToInternet(): Boolean {
        return false
    }
}
