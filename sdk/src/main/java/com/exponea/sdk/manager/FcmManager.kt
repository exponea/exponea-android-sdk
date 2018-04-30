package com.exponea.sdk.manager

interface FcmManager {
    fun getFcmToken(): String
    fun trackFcmToken()
}