package com.exponea.sdk.services

interface AuthorizationProvider {
    fun getAuthorizationToken(): String?
}
