package com.exponea.sdk.manager

import com.exponea.sdk.models.CustomerIds

interface FcmManager {
    fun getFcmToken(): String
    fun trackFcmToken()
}