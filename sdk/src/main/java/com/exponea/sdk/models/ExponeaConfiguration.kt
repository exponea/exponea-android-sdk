package com.exponea.sdk.models

data class ExponeaConfiguration(
        var projectToken: String = "",
        var projectTokenRouteMap: HashMap<Route, MutableList<String>> = hashMapOf(),
        var authorization: String? = null,
        var baseURL: String = Constants.Repository.baseURL,
        var contentType: String = Constants.Repository.contentType,
        var maxTries: Int = 10
//        var sessionTimeout: Double,
//        var lastSessionStarted: Double,
//        var lastSessionEnded: Double,
//        var autoSessionTracking: Boolean
) {
}