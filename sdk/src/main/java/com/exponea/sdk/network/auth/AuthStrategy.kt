package com.exponea.sdk.network.auth

internal sealed class AuthStrategy {

    data class PublicApiKey(val apiToken: String?) : AuthStrategy()

    data class Token(val mode: AuthMode = AuthMode.OPTIONAL) : AuthStrategy()

    data class LegacyToken(
        // fallback when legacy token provider is not defined
        val publicApiKey: String?,
        val mode: AuthMode = AuthMode.OPTIONAL
    ) : AuthStrategy()

    object None : AuthStrategy()
}

internal enum class AuthMode { REQUIRED, OPTIONAL }
