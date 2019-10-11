package com.exponea.sdk.manager

internal object ConnectionManagerMock: ConnectionManager {
    override fun isConnectedToInternet(): Boolean {
        return true
    }
}