package com.exponea.sdk.models

data class Configuration(var projectToken: String?,
                         var authorization: String?,
                         var baseURL: String = Constants.Repository.baseURL,
                         var contentType: String = Constants.Repository.contentType,
                         var sessionTimeout: Double,
                         var lastSessionStarted: Double,
                         var lastSessionEndend: Double,
                         var autoSessionTracking: Boolean)