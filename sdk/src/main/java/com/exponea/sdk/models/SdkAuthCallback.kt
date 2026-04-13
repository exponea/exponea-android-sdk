package com.exponea.sdk.models

interface SdkAuthCallback {
    fun onAuthFailure(error: SdkAuthError)
}
