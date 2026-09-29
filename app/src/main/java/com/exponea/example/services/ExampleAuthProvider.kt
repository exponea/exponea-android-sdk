package com.exponea.example.services

import com.exponea.example.managers.CustomerTokenStorage
import com.exponea.sdk.services.AuthorizationProvider

class ExampleAuthProvider : AuthorizationProvider {
    override fun getAuthorizationToken(): String? {
        return CustomerTokenStorage.INSTANCE.retrieveJwtToken()
    }
}
