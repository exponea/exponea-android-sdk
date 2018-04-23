package com.exponea.sdk.models

data class ExponeaConfiguration(
        var projectToken: String? = null,
        var projectTokenRouteMap: HashMap<Route, MutableList<String>> = hashMapOf(),
        var authorization: String? = null,
        var baseURL: String = Constants.Repository.baseURL,
        var contentType: String = Constants.Repository.contentType
//        var sessionTimeout: Double,
//        var lastSessionStarted: Double,
//        var lastSessionEnded: Double,
//        var autoSessionTracking: Boolean
) {
}