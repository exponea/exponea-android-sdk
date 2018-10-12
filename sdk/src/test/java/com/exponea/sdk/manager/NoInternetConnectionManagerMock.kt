package com.exponea.sdk.manager

object NoInternetConnectionManagerMock: ConnectionManager {

    override fun isConnectedToInternet(): Boolean {
        return false
    }

}