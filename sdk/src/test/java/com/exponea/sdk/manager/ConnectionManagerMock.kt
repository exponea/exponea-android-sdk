package com.exponea.sdk.manager

object ConnectionManagerMock: ConnectionManager {
    override fun isConnectedToInternet(): Boolean {
        return true
    }
}